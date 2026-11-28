import 'package:flutter/foundation.dart';
import '../../../../core/utils/csv_downloader.dart';
import '../../domain/entities/annual_tax_summary.dart';
import '../../domain/entities/tax_declaration_report.dart';
import '../../domain/repositories/tax_repository.dart';

typedef CsvDownloader = Future<void> Function({
  required String content,
  required String fileName,
});

enum TaxTab { bensEDireitos, apuracaoMensal }

class TaxProvider extends ChangeNotifier {
  final TaxRepository repository;
  final CsvDownloader _csvDownloader;

  TaxTab _activeTab = TaxTab.bensEDireitos;
  TaxDeclarationReport? _report;
  AnnualTaxSummary? _monthlySummary;
  List<int> _availableYears = [];
  int? _selectedYear;
  String? _selectedProfileId;
  bool _isLoadingReport = false;
  bool _isLoadingMonthly = false;
  bool _isExporting = false;
  String? _errorMessage;

  TaxProvider({
    required this.repository,
    CsvDownloader? csvDownloader,
  }) : _csvDownloader = csvDownloader ?? downloadCsvFile;

  TaxTab get activeTab => _activeTab;
  TaxDeclarationReport? get report => _report;
  AnnualTaxSummary? get monthlySummary => _monthlySummary;
  List<int> get availableYears => _availableYears;
  int? get selectedYear => _selectedYear;
  String? get selectedProfileId => _selectedProfileId;
  bool get isLoading => _isLoadingReport || _isLoadingMonthly;
  bool get isExporting => _isExporting;
  String? get errorMessage => _errorMessage;

  void setActiveTab(TaxTab tab) {
    if (_activeTab == tab) return;
    _activeTab = tab;
    notifyListeners();
  }

  Future<void> init() async {
    await loadAvailableYears();
    await Future.wait([
      loadReport(),
      loadMonthlyCapitalGains(),
    ]);
  }

  Future<void> loadAvailableYears() async {
    try {
      final years = await repository.getAvailableYears(profileId: _selectedProfileId);
      _availableYears = years;
      final defaultYear = DateTime.now().year - 1;
      if (_selectedYear == null || !_availableYears.contains(_selectedYear)) {
        if (_availableYears.contains(defaultYear)) {
          _selectedYear = defaultYear;
        } else if (_availableYears.isNotEmpty) {
          _selectedYear = _availableYears.first;
        } else {
          _selectedYear = defaultYear;
        }
      }
      notifyListeners();
    } catch (e) {
      debugPrint('Erro ao carregar anos disponíveis: $e');
      _selectedYear ??= DateTime.now().year - 1;
    }
  }

  Future<void> setYear(int year) async {
    if (_selectedYear == year) return;
    _selectedYear = year;
    notifyListeners();
    await Future.wait([
      loadReport(),
      loadMonthlyCapitalGains(),
    ]);
  }

  Future<void> setProfileId(String? profileId) async {
    if (_selectedProfileId == profileId) return;
    _selectedProfileId = profileId;
    notifyListeners();
    await loadAvailableYears();
    await Future.wait([
      loadReport(),
      loadMonthlyCapitalGains(),
    ]);
  }

  Future<void> loadReport() async {
    _isLoadingReport = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await repository.getBensEDireitos(
        year: _selectedYear,
        profileId: _selectedProfileId,
      );
      _report = result;
      _isLoadingReport = false;
      notifyListeners();
    } catch (e) {
      _isLoadingReport = false;
      _errorMessage = 'Falha ao carregar o informe de Bens e Direitos: ${e.toString()}';
      notifyListeners();
    }
  }

  Future<void> loadMonthlyCapitalGains() async {
    _isLoadingMonthly = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await repository.getMonthlyCapitalGains(
        year: _selectedYear,
        profileId: _selectedProfileId,
      );
      _monthlySummary = result;
      _isLoadingMonthly = false;
      notifyListeners();
    } catch (e) {
      _isLoadingMonthly = false;
      _errorMessage = 'Falha ao carregar a apuração mensal de ganhos de capital: ${e.toString()}';
      notifyListeners();
    }
  }

  Future<String?> exportCsv() {
    final yearSuffix = _selectedYear?.toString() ?? 'atual';
    return _exportCsvWith(
      () => repository.exportBensEDireitosCsv(
        year: _selectedYear,
        profileId: _selectedProfileId,
      ),
      'bens-e-direitos-$yearSuffix.csv',
    );
  }

  Future<String?> exportMonthlyCsv() {
    final yearSuffix = _selectedYear?.toString() ?? 'atual';
    return _exportCsvWith(
      () => repository.exportMonthlyCapitalGainsCsv(
        year: _selectedYear,
        profileId: _selectedProfileId,
      ),
      'ganhos-capital-mensal-$yearSuffix.csv',
    );
  }

  Future<String?> _exportCsvWith(
    Future<String> Function() fetcher,
    String fileName,
  ) async {
    _isExporting = true;
    notifyListeners();

    try {
      final csv = await fetcher();
      await _csvDownloader(content: csv, fileName: fileName);
      _isExporting = false;
      notifyListeners();
      return csv;
    } catch (e) {
      _isExporting = false;
      notifyListeners();
      return null;
    }
  }
}
