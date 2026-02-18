import 'package:flutter/foundation.dart';
import '../../domain/entities/ranked_company.dart';
import '../../domain/usecases/get_ranking_usecase.dart';

enum RankingState { idle, loading, success, error }

class RankingProvider extends ChangeNotifier {
  final GetRankingUseCase getRankingUseCase;

  RankingState _state = RankingState.idle;
  List<RankedCompany> _companies = [];
  String? _errorMessage;

  RankingProvider(this.getRankingUseCase);

  RankingState get state => _state;
  List<RankedCompany> get companies => _companies;
  String? get errorMessage => _errorMessage;

  Future<void> fetchRanking() async {
    _setState(RankingState.loading);
    _errorMessage = null;

    try {
      _companies = await getRankingUseCase();
      _setState(RankingState.success);
    } catch (e) {
      _errorMessage = e.toString();
      _setState(RankingState.error);
    }
  }

  void _setState(RankingState newState) {
    _state = newState;
    notifyListeners();
  }
}
