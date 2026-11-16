import 'package:flutter/foundation.dart';
import '../../domain/entities/dividend_announcement.dart';
import '../../domain/entities/relevant_fact.dart';
import '../../domain/repositories/dividend_repository.dart';

class DividendProvider extends ChangeNotifier {
  final DividendRepository repository;

  bool _isLoading = false;
  String? _errorMessage;
  List<DividendAnnouncement> _schedule = [];
  List<RelevantFact> _facts = [];
  bool _onlyMyAssets = false;
  String? _selectedTicker;
  int _selectedTab = 0; // 0 = Agenda de Proventos, 1 = Fatos Relevantes

  DividendProvider({required this.repository});

  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  List<DividendAnnouncement> get schedule => _schedule;
  List<RelevantFact> get facts => _facts;
  bool get onlyMyAssets => _onlyMyAssets;
  String? get selectedTicker => _selectedTicker;
  int get selectedTab => _selectedTab;

  double get totalEstimatedNext30Days {
    final now = DateTime.now();
    final in30Days = now.add(const Duration(days: 30));

    double total = 0.0;
    for (final item in _schedule) {
      final date = item.paymentDate ?? item.comDate;
      // Considera eventos futuros ou previstos para os próximos 30 dias
      if (!date.isBefore(now.subtract(const Duration(days: 1))) && !date.isAfter(in30Days)) {
        if (item.estimatedNetTotal != null && item.estimatedNetTotal! > 0) {
          total += item.estimatedNetTotal!;
        } else if (item.estimatedTotal != null && item.estimatedTotal! > 0) {
          total += item.estimatedTotal!;
        }
      }
    }
    return total;
  }

  void setSelectedTab(int index) {
    if (_selectedTab != index) {
      _selectedTab = index;
      notifyListeners();
    }
  }

  void toggleOnlyMyAssets() {
    _onlyMyAssets = !_onlyMyAssets;
    notifyListeners();
    loadData();
  }

  void setSelectedTicker(String? ticker) {
    _selectedTicker = ticker;
    notifyListeners();
    loadData();
  }

  Future<void> loadData() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final results = await Future.wait([
        repository.getDividendSchedule(
          ticker: _selectedTicker,
          onlyMyTickers: _onlyMyAssets,
        ),
        repository.getRelevantFacts(
          ticker: _selectedTicker,
          onlyMyTickers: _onlyMyAssets,
        ),
      ]);

      _schedule = results[0] as List<DividendAnnouncement>;
      _facts = results[1] as List<RelevantFact>;
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      _isLoading = false;
      notifyListeners();
    }
  }
}
