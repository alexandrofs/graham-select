import 'package:flutter/material.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/portfolio_tag.dart';
import '../../domain/repositories/tag_repository.dart';

enum TagState { initial, loading, loaded, error }

const List<String> kTagColorPalette = <String>[
  '#6366F1',
  '#10B981',
  '#8B5CF6',
  '#F59E0B',
  '#EC4899',
  '#06B6D4',
];

const Map<String, String> kTagColorLabels = <String, String>{
  '#6366F1': 'Índigo',
  '#10B981': 'Esmeralda',
  '#8B5CF6': 'Violeta',
  '#F59E0B': 'Âmbar',
  '#EC4899': 'Rosa',
  '#06B6D4': 'Ciano',
};

String tagColorLabel(String hexColor) {
  final normalized = hexColor.trim().toUpperCase();
  return kTagColorLabels[normalized] ?? normalized;
}

class TagProvider extends ChangeNotifier {
  final TagRepository repository;

  TagProvider({required this.repository});

  TagState _state = TagState.initial;
  List<PortfolioTag> _tags = [];
  Map<String, List<PortfolioTag>> _tagsByTicker = const {};
  String? _selectedFilterTagId;
  bool _isSubmitting = false;
  String? _errorMessage;
  int _loadGeneration = 0;

  TagState get state => _state;
  List<PortfolioTag> get tags => List.unmodifiable(_tags);
  String? get selectedFilterTagId => _selectedFilterTagId;
  bool get isSubmitting => _isSubmitting;
  String? get errorMessage => _errorMessage;

  PortfolioTag? get selectedFilterTag {
    if (_selectedFilterTagId == null) return null;
    for (final tag in _tags) {
      if (tag.id == _selectedFilterTagId) return tag;
    }
    return null;
  }

  Set<String> get filteredTickersForSelectedTag {
    final selected = selectedFilterTag;
    if (selected == null) return const <String>{};
    return selected.tickers
        .map((t) => t.trim().toUpperCase())
        .where((t) => t.isNotEmpty)
        .toSet();
  }

  List<PortfolioTag> tagsForTicker(String ticker) {
    final normalized = ticker.trim().toUpperCase();
    if (normalized.isEmpty) return const <PortfolioTag>[];
    return _tagsByTicker[normalized] ?? const <PortfolioTag>[];
  }

  void clearError() {
    if (_errorMessage == null) return;
    _errorMessage = null;
    notifyListeners();
  }

  void reset() {
    _loadGeneration++;
    final hadState = _state != TagState.initial ||
        _tags.isNotEmpty ||
        _tagsByTicker.isNotEmpty ||
        _selectedFilterTagId != null ||
        _isSubmitting ||
        _errorMessage != null;
    _state = TagState.initial;
    _tags = [];
    _tagsByTicker = const {};
    _selectedFilterTagId = null;
    _isSubmitting = false;
    _errorMessage = null;
    if (hadState) {
      notifyListeners();
    }
  }

  void selectFilterTag(String? tagId) {
    if (_selectedFilterTagId == tagId) return;
    _selectedFilterTagId = tagId;
    notifyListeners();
  }

  static Color parseColor(String? hexColor) {
    if (hexColor == null) return AppTheme.primaryColor;
    final normalized = hexColor.trim().toUpperCase();
    final match = RegExp(r'^#([0-9A-F]{6})$').firstMatch(normalized);
    if (match == null) return AppTheme.primaryColor;
    final rgb = int.parse(match.group(1)!, radix: 16);
    return Color(0xFF000000 | rgb);
  }

  Future<void> loadTags({bool silent = false}) async {
    final requestGeneration = ++_loadGeneration;
    if (!silent) {
      _state = TagState.loading;
      _errorMessage = null;
      notifyListeners();
    }

    try {
      final loaded = await repository.fetchTags();
      if (requestGeneration != _loadGeneration) return;
      _updateTags(loaded);
      if (_selectedFilterTagId != null &&
          !_tags.any((t) => t.id == _selectedFilterTagId)) {
        _selectedFilterTagId = null;
      }
      _state = TagState.loaded;
      _errorMessage = null;
    } catch (e, stackTrace) {
      if (requestGeneration != _loadGeneration) return;
      debugPrint('[TagProvider] ERROR loading tags: $e\n$stackTrace');
      _errorMessage = _cleanError(e);
      if (!silent || _tags.isEmpty) {
        _state = TagState.error;
      }
    }
    notifyListeners();
  }

  Future<PortfolioTag?> createTag({required String name, String? color}) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final created = await repository.createTag(name: name, color: color);
      _loadGeneration++;
      final updatedList = [
        ..._tags.where((t) => t.id != created.id),
        created,
      ];
      _updateTags(updatedList);
      _state = TagState.loaded;
      _isSubmitting = false;
      notifyListeners();
      return created;
    } catch (e, stackTrace) {
      debugPrint('[TagProvider] ERROR creating tag: $e\n$stackTrace');
      _errorMessage = _cleanError(e);
      _isSubmitting = false;
      notifyListeners();
      return null;
    }
  }

  Future<PortfolioTag?> renameTag({
    required String tagId,
    required String name,
    String? color,
  }) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final updated = await repository.updateTag(
        tagId: tagId,
        name: name,
        color: color,
      );
      _loadGeneration++;
      final updatedList = _tags.map((t) => t.id == tagId ? updated : t).toList();
      if (!updatedList.any((t) => t.id == updated.id)) {
        updatedList.add(updated);
      }
      _updateTags(updatedList);
      _state = TagState.loaded;
      _isSubmitting = false;
      notifyListeners();
      return updated;
    } catch (e, stackTrace) {
      debugPrint('[TagProvider] ERROR renaming tag: $e\n$stackTrace');
      _errorMessage = _cleanError(e);
      _isSubmitting = false;
      notifyListeners();
      return null;
    }
  }

  Future<bool> deleteTag(String tagId) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      await repository.deleteTag(tagId);
      _loadGeneration++;
      _updateTags(_tags.where((t) => t.id != tagId).toList());
      if (_selectedFilterTagId == tagId) {
        _selectedFilterTagId = null;
      }
      _state = TagState.loaded;
      _isSubmitting = false;
      notifyListeners();
      return true;
    } catch (e, stackTrace) {
      debugPrint('[TagProvider] ERROR deleting tag: $e\n$stackTrace');
      _errorMessage = _cleanError(e);
      _isSubmitting = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  }) async {
    final normalizedTicker = ticker.trim().toUpperCase();
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final assignedTags = await repository.setAssetTags(
        ticker: normalizedTicker,
        tagIds: tagIds,
      );
      _loadGeneration++;
      final selectedIds = assignedTags.map((t) => t.id).toSet();

      final updatedList = <PortfolioTag>[];
      final knownIds = <String>{};
      for (final tag in _tags) {
        knownIds.add(tag.id);
        final nextTickers = tag.tickers
            .map((t) => t.trim().toUpperCase())
            .where((t) => t.isNotEmpty && t != normalizedTicker)
            .toSet();
        if (selectedIds.contains(tag.id)) {
          nextTickers.add(normalizedTicker);
        }
        final sortedTickers = nextTickers.toList()..sort();
        updatedList.add(
          tag.copyWith(
            tickers: List<String>.unmodifiable(sortedTickers),
            assetCount: sortedTickers.length,
          ),
        );
      }

      for (final assigned in assignedTags) {
        if (!knownIds.contains(assigned.id)) {
          updatedList.add(
            assigned.copyWith(
              tickers: List<String>.unmodifiable([normalizedTicker]),
              assetCount: 1,
            ),
          );
        }
      }

      _updateTags(updatedList);
      _state = TagState.loaded;
      _isSubmitting = false;
      notifyListeners();
      return true;
    } catch (e, stackTrace) {
      debugPrint('[TagProvider] ERROR setting asset tags: $e\n$stackTrace');
      _errorMessage = _cleanError(e);
      _isSubmitting = false;
      notifyListeners();
      return false;
    }
  }

  Future<PortfolioTag?> createAndAssignTag({
    required String ticker,
    required String name,
    String? color,
  }) async {
    final created = await createTag(name: name, color: color);
    if (created == null) return null;

    final currentIds = tagsForTicker(ticker).map((t) => t.id).toSet()
      ..add(created.id);
    final success = await setAssetTags(
      ticker: ticker,
      tagIds: currentIds.toList(),
    );
    return success ? created : null;
  }

  void _updateTags(List<PortfolioTag> nextTags) {
    _tags = _sortedTags(nextTags);
    final index = <String, List<PortfolioTag>>{};
    for (final tag in _tags) {
      final seenForTag = <String>{};
      for (final rawTicker in tag.tickers) {
        final normalized = rawTicker.trim().toUpperCase();
        if (normalized.isEmpty || !seenForTag.add(normalized)) continue;
        index.putIfAbsent(normalized, () => <PortfolioTag>[]).add(tag);
      }
    }
    _tagsByTicker = {
      for (final entry in index.entries)
        entry.key: List<PortfolioTag>.unmodifiable(entry.value),
    };
  }

  List<PortfolioTag> _sortedTags(List<PortfolioTag> input) {
    final copy = List<PortfolioTag>.from(input);
    copy.sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
    return copy;
  }

  String _cleanError(Object error) {
    return error.toString().replaceFirst('Exception: ', '');
  }
}
