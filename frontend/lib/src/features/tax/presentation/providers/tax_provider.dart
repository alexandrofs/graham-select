import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../domain/entities/tax_declaration_report.dart';
import '../../domain/repositories/tax_repository.dart';

class TaxProvider extends ChangeNotifier {
  final TaxRepository repository;

  TaxDeclarationReport? _report;
  List<int> _availableYears = [];
  int? _selectedYear;
  String? _selectedProfileId;
  bool _isLoading = false;
  bool _isExporting = false;
  String? _errorMessage;

  TaxProvider({required this.repository});

  TaxDeclarationReport? get report => _report;
  List<int> get availableYears => _availableYears;
  int? get selectedYear => _selectedYear;
  String? get selectedProfileId => _selectedProfileId;
  bool get isLoading => _isLoading;
  bool get isExporting => _isExporting;
  String? get errorMessage => _errorMessage;

  Future<void> init() async {
    await loadAvailableYears();
    await loadReport();
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
    await loadReport();
  }

  Future<void> setProfileId(String? profileId) async {
    if (_selectedProfileId == profileId) return;
    _selectedProfileId = profileId;
    notifyListeners();
    await loadAvailableYears();
    await loadReport();
  }

  Future<void> loadReport() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await repository.getBensEDireitos(
        year: _selectedYear,
        profileId: _selectedProfileId,
      );
      _report = result;
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _isLoading = false;
      _errorMessage = 'Falha ao carregar o informe de Bens e Direitos: ${e.toString()}';
      notifyListeners();
    }
  }

  Future<String?> exportCsv() async {
    _isExporting = true;
    notifyListeners();

    try {
      final csv = await repository.exportBensEDireitosCsv(
        year: _selectedYear,
        profileId: _selectedProfileId,
      );
      final uri = Uri.dataFromString(
        csv,
        mimeType: 'text/csv',
        encoding: utf8,
      );
      await launchUrl(uri);
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
