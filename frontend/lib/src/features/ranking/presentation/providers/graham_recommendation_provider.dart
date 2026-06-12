import 'package:flutter/foundation.dart';
import '../../domain/entities/graham_recommendation.dart';
import '../../domain/repositories/graham_recommendation_repository.dart';

enum RecommendationState { idle, loading, success, error }

class GrahamRecommendationProvider extends ChangeNotifier {
  final GrahamRecommendationRepository repository;

  RecommendationState _state = RecommendationState.idle;
  List<GrahamRecommendation> _recommendations = [];
  String? _errorMessage;
  bool _isTriggering = false;

  GrahamRecommendationProvider({required this.repository});

  RecommendationState get state => _state;
  List<GrahamRecommendation> get recommendations => _recommendations;
  String? get errorMessage => _errorMessage;
  bool get isTriggering => _isTriggering;

  Future<void> fetchRecommendations() async {
    _setState(RecommendationState.loading);
    _errorMessage = null;

    try {
      _recommendations = await repository.getRecommendations();
      _setState(RecommendationState.success);
    } catch (e) {
      _errorMessage = e.toString();
      _setState(RecommendationState.error);
    }
  }

  Future<void> triggerCalculation() async {
    _isTriggering = true;
    _errorMessage = null;
    notifyListeners();
 
    try {
      await repository.triggerCalculation();
      
      final oldRecs = List<GrahamRecommendation>.from(_recommendations);
      List<GrahamRecommendation> freshRecs = oldRecs;
 
      for (int i = 0; i < 5; i++) {
        await Future.delayed(const Duration(seconds: 1));
        freshRecs = await repository.getRecommendations();
        if (!_areListsEqual(oldRecs, freshRecs)) {
          break;
        }
      }
 
      _recommendations = freshRecs;
      _setState(RecommendationState.success);
    } catch (e) {
      _errorMessage = e.toString();
      _setState(RecommendationState.error);
    } finally {
      _isTriggering = false;
      notifyListeners();
    }
  }
 
  bool _areListsEqual(List<GrahamRecommendation> a, List<GrahamRecommendation> b) {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i].ticker != b[i].ticker ||
          a[i].currentPrice != b[i].currentPrice ||
          a[i].intrinsicValue != b[i].intrinsicValue ||
          a[i].marginOfSafety != b[i].marginOfSafety ||
          a[i].recommendationScore != b[i].recommendationScore) {
        return false;
      }
    }
    return true;
  }
 
  void reset() {
    _state = RecommendationState.idle;
    _recommendations = [];
    _errorMessage = null;
    _isTriggering = false;
    notifyListeners();
  }
 
  void _setState(RecommendationState newState) {
    _state = newState;
    notifyListeners();
  }
}
