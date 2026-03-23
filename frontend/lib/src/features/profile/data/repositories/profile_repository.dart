import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';

class ProfileRepository {
  final ApiClient _apiClient;

  ProfileRepository(this._apiClient);

  Future<UserProfile> getProfile() async {
    try {
      final response = await _apiClient.get('/api/v1/users/me');
      
      if (response.statusCode == 200) {
        return UserProfile.fromJson(response.data);
      } else {
        throw Exception('Failed to load profile: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error fetching profile: $e');
    }
  }
}
