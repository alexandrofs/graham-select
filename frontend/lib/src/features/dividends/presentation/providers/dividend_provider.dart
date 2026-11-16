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
  DateTime? _startDate;
  DateTime? _endDate;
  int _selectedTab = 0; // 0 = Agenda de Proventos, 1 = Fatos Relevantes

  DividendProvider({required this.repository});

  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  List<DividendAnnouncement> get schedule => _schedule;
  List<RelevantFact> get facts => _facts;
  bool get onlyMyAssets => _onlyMyAssets;
  String? get selectedTicker => _selectedTicker;
  DateTime? get startDate => _startDate;
  DateTime? get endDate => _endDate;
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

  void setDateRange(DateTime? start, DateTime? end) {
    _startDate = start;
    _endDate = end;
    notifyListeners();
    loadData();
  }

  void clearDateRange() {
    _startDate = null;
    _endDate = null;
    notifyListeners();
    loadData();
  }

  Future<void> loadData() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    String? scheduleError;
    String? factsError;

    try {
      _schedule = await repository.getDividendSchedule(
        ticker: _selectedTicker,
        startDate: _startDate,
        endDate: _endDate,
        onlyMyTickers: _onlyMyAssets,
      );
    } catch (e) {
      scheduleError = e.toString().replaceFirst('Exception: ', '');
    }

    try {
      _facts = await repository.getRelevantFacts(
        ticker: _selectedTicker,
        onlyMyTickers: _onlyMyAssets,
      );
    } catch (e) {
      factsError = e.toString().replaceFirst('Exception: ', '');
    }

    if (scheduleError != null && factsError != null) {
      _errorMessage = scheduleError;
    } else if (scheduleError != null && _selectedTab == 0) {
      _errorMessage = scheduleError;
    } else if (factsError != null && _selectedTab == 1) {
      _errorMessage = factsError;
    }

    _isLoading = false;
    notifyListeners();
  }
}
