import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';

void main() {
  group('UserProfile Model', () {
    test('should parse from JSON correctly', () {
      final json = {
        'id': 1,
        'email': 'user@example.com',
        'fullName': 'Test User',
        'tier': 'TRIAL',
        'trialEndsAt': '2026-04-22T20:24:51Z',
        'daysRemaining': 30,
      };

      final profile = UserProfile.fromJson(json);

      expect(profile.id, 1);
      expect(profile.email, 'user@example.com');
      expect(profile.fullName, 'Test User');
      expect(profile.tier, 'TRIAL');
      expect(profile.trialEndsAt, DateTime.parse('2026-04-22T20:24:51Z'));
      expect(profile.daysRemaining, 30);
    });

    test('should parse from JSON with null trial fields', () {
      final json = {
        'id': 1,
        'email': 'user@example.com',
        'fullName': 'Test User',
        'tier': 'FREE',
        'trialEndsAt': null,
        'daysRemaining': null,
      };

      final profile = UserProfile.fromJson(json);

      expect(profile.id, 1);
      expect(profile.tier, 'FREE');
      expect(profile.trialEndsAt, null);
      expect(profile.daysRemaining, null);
    });

    test('should convert to JSON correctly', () {
      final profile = UserProfile(
        id: 1,
        email: 'user@example.com',
        fullName: 'Test User',
        tier: 'PREMIUM',
        trialEndsAt: DateTime.parse('2026-04-22T20:24:51Z'),
        daysRemaining: 15,
      );

      final json = profile.toJson();

      expect(json['id'], 1);
      expect(json['email'], 'user@example.com');
      expect(json['fullName'], 'Test User');
      expect(json['tier'], 'PREMIUM');
      expect(json['trialEndsAt'], '2026-04-22T20:24:51.000Z');
      expect(json['daysRemaining'], 15);
    });
  });
}
