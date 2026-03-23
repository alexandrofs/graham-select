import 'package:flutter/material.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';
import 'package:frontend/src/features/profile/data/repositories/profile_repository.dart';

enum ProfileState { idle, loading, success, error }

class ProfileProvider extends ChangeNotifier {
  final ProfileRepository _repository;

  ProfileProvider(this._repository);

  ProfileState _state = ProfileState.idle;
  ProfileState get state => _state;

  UserProfile? _profile;
  UserProfile? get profile => _profile;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  Future<void> fetchProfile() async {
    _state = ProfileState.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      _profile = await _repository.getProfile();
      _state = ProfileState.success;
    } catch (e) {
      _state = ProfileState.error;
      _errorMessage = e.toString();
    } finally {
      notifyListeners();
    }
  }
}
