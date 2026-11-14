import 'package:flutter/foundation.dart';
import '../../data/datasources/family_profile_remote_datasource.dart';
import '../../domain/entities/family_profile_entity.dart';
import '../../domain/repositories/family_profile_repository.dart';

enum FamilyProfileState { initial, loading, loaded, error, premiumRequired }

class FamilyProfileProvider extends ChangeNotifier {
  final FamilyProfileRepository repository;

  FamilyProfileProvider({required this.repository});

  FamilyProfileState _state = FamilyProfileState.initial;
  List<FamilyProfileEntity> _profiles = [];
  String? _errorMessage;
  bool _isSubmitting = false;

  FamilyProfileState get state => _state;
  List<FamilyProfileEntity> get profiles => List.unmodifiable(_profiles);
  String? get errorMessage => _errorMessage;
  bool get isSubmitting => _isSubmitting;

  Future<void> fetchProfiles({bool silent = false}) async {
    if (!silent) {
      _state = FamilyProfileState.loading;
      _errorMessage = null;
      notifyListeners();
    }

    try {
      _profiles = await repository.fetchProfiles();
      _state = FamilyProfileState.loaded;
      _errorMessage = null;
    } on PremiumRequiredException catch (e) {
      _profiles = [];
      _state = FamilyProfileState.premiumRequired;
      _errorMessage = e.message;
    } catch (e) {
      _state = FamilyProfileState.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  Future<bool> linkProfile({
    required String cpf,
    required String name,
    String? relationship,
  }) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final created = await repository.linkProfile(
        cpf: cpf,
        name: name,
        relationship: relationship,
      );
      _profiles = [..._profiles, created];
      _state = FamilyProfileState.loaded;
      _isSubmitting = false;
      notifyListeners();
      return true;
    } on PremiumRequiredException catch (e) {
      _state = FamilyProfileState.premiumRequired;
      _errorMessage = e.message;
      _isSubmitting = false;
      notifyListeners();
      return false;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      _isSubmitting = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> unlinkProfile(String profileId) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      await repository.unlinkProfile(profileId);
      _profiles = _profiles.where((p) => p.id != profileId).toList();
      _state = FamilyProfileState.loaded;
      _isSubmitting = false;
      notifyListeners();
      return true;
    } on PremiumRequiredException catch (e) {
      _state = FamilyProfileState.premiumRequired;
      _errorMessage = e.message;
      _isSubmitting = false;
      notifyListeners();
      return false;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      _isSubmitting = false;
      notifyListeners();
      return false;
    }
  }
}

