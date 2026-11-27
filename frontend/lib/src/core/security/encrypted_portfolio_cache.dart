import 'dart:async';
import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../features/portfolio/data/models/custody_position_model.dart';
import '../../features/portfolio/data/models/portfolio_summary_model.dart';
import '../../features/portfolio/domain/entities/custody_position.dart';
import '../../features/portfolio/domain/entities/portfolio_summary.dart';

/// Snapshot offline criptografado do Dashboard para um escopo (usuário + tag).
class PortfolioSnapshot {
  final PortfolioSummaryModel? summary;
  final List<CustodyPositionModel>? positions;
  final DateTime? metaPriceUpdatedAt;
  final int untaggedAssetsCount;
  final List<String> untaggedTickers;
  final DateTime lastSyncedAt;

  const PortfolioSnapshot({
    this.summary,
    this.positions,
    this.metaPriceUpdatedAt,
    this.untaggedAssetsCount = 0,
    this.untaggedTickers = const [],
    required this.lastSyncedAt,
  });

  Map<String, dynamic> toJson() {
    return {
      if (summary != null) 'summary': summary!.toJson(),
      if (positions != null)
        'positions': positions!.map((p) => p.toJson()).toList(),
      'metaPriceUpdatedAt': metaPriceUpdatedAt?.toUtc().toIso8601String(),
      'untaggedAssetsCount': untaggedAssetsCount,
      'untaggedTickers': untaggedTickers,
      'lastSyncedAt': lastSyncedAt.toUtc().toIso8601String(),
    };
  }

  factory PortfolioSnapshot.fromJson(Map<String, dynamic> json) {
    final summaryJson = json['summary'] as Map<String, dynamic>?;
    final positionsJson = json['positions'] as List<dynamic>?;
    final metaUpdatedRaw = json['metaPriceUpdatedAt'] as String?;
    final untaggedRaw = json['untaggedTickers'] as List<dynamic>?;

    return PortfolioSnapshot(
      summary: summaryJson != null
          ? PortfolioSummaryModel.fromJson(summaryJson)
          : null,
      positions: positionsJson
          ?.map((e) => CustodyPositionModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      metaPriceUpdatedAt: metaUpdatedRaw != null
          ? DateTime.parse(metaUpdatedRaw)
          : null,
      untaggedAssetsCount: (json['untaggedAssetsCount'] as num?)?.toInt() ?? 0,
      untaggedTickers: untaggedRaw?.map((e) => e.toString()).toList() ?? const [],
      lastSyncedAt: DateTime.parse(json['lastSyncedAt'] as String).toUtc(),
    );
  }
}

/// Cache local criptografado sobre [FlutterSecureStorage] (Keychain no iOS /
/// EncryptedSharedPreferences no Android) para o último snapshot válido do Dashboard.
class EncryptedPortfolioCache {
  static const String _keyPrefix = 'kerno_portfolio_snapshot_';
  static const FlutterSecureStorage defaultSecureStorage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  final FlutterSecureStorage _storage;
  final DateTime Function() _now;
  final Set<String> _trackedKeys = <String>{};
  Future<void> _pendingOp = Future<void>.value();

  EncryptedPortfolioCache({
    FlutterSecureStorage? storage,
    DateTime Function()? now,
  })  : _storage = storage ?? defaultSecureStorage,
        _now = now ?? (() => DateTime.now().toUtc());

  Future<T> _synchronized<T>(Future<T> Function() action) {
    final completer = Completer<T>();
    _pendingOp = _pendingOp.then((_) async {
      try {
        completer.complete(await action());
      } catch (e, st) {
        completer.completeError(e, st);
      }
    });
    return completer.future;
  }

  String _buildKey({String? userId, String? tagId}) {
    final userScope = Uri.encodeComponent(
      (userId == null || userId.trim().isEmpty) ? 'default' : userId.trim(),
    );
    final tagScope = Uri.encodeComponent(
      (tagId == null || tagId.trim().isEmpty) ? 'global' : tagId.trim(),
    );
    return '$_keyPrefix$userScope:$tagScope';
  }

  PortfolioSummaryModel _toSummaryModel(PortfolioSummary summary) {
    if (summary is PortfolioSummaryModel) return summary;
    return PortfolioSummaryModel(
      totalEquity: summary.totalEquity,
      grossYieldPercentage: summary.grossYieldPercentage,
      accumulatedDividends: summary.accumulatedDividends,
      monthlyProjection: summary.monthlyProjection,
    );
  }

  CustodyPositionModel _toCustodyModel(CustodyPosition pos) {
    if (pos is CustodyPositionModel) return pos;
    return CustodyPositionModel(
      ticker: pos.ticker,
      quantity: pos.quantity,
      averagePrice: pos.averagePrice,
      currentPrice: pos.currentPrice,
      marketValue: pos.marketValue,
      gainLossPercentage: pos.gainLossPercentage,
      priceSource: pos.priceSource,
      priceUpdatedAt: pos.priceUpdatedAt,
      assetClass: pos.assetClass,
    );
  }

  Future<void> saveSummary({
    required PortfolioSummary summary,
    String? userId,
    String? tagId,
  }) {
    return _synchronized(() async {
      final existing = await loadSnapshot(userId: userId, tagId: tagId);
      final updated = PortfolioSnapshot(
        summary: _toSummaryModel(summary),
        positions: existing?.positions,
        metaPriceUpdatedAt: existing?.metaPriceUpdatedAt,
        untaggedAssetsCount: existing?.untaggedAssetsCount ?? 0,
        untaggedTickers: existing?.untaggedTickers ?? const [],
        lastSyncedAt: _now().toUtc(),
      );
      await _writeSnapshot(updated, userId: userId, tagId: tagId);
    });
  }

  Future<void> saveCustody({
    required List<CustodyPosition> positions,
    DateTime? metaPriceUpdatedAt,
    int untaggedAssetsCount = 0,
    List<String> untaggedTickers = const [],
    String? userId,
    String? tagId,
  }) {
    return _synchronized(() async {
      final existing = await loadSnapshot(userId: userId, tagId: tagId);
      final updated = PortfolioSnapshot(
        summary: existing?.summary,
        positions: positions.map(_toCustodyModel).toList(),
        metaPriceUpdatedAt: metaPriceUpdatedAt,
        untaggedAssetsCount: untaggedAssetsCount,
        untaggedTickers: List<String>.from(untaggedTickers),
        lastSyncedAt: _now().toUtc(),
      );
      await _writeSnapshot(updated, userId: userId, tagId: tagId);
    });
  }

  Future<void> _writeSnapshot(
    PortfolioSnapshot snapshot, {
    String? userId,
    String? tagId,
  }) async {
    final key = _buildKey(userId: userId, tagId: tagId);
    _trackedKeys.add(key);
    await _storage.write(key: key, value: jsonEncode(snapshot.toJson()));
  }

  Future<PortfolioSnapshot?> loadSnapshot({
    String? userId,
    String? tagId,
  }) async {
    final key = _buildKey(userId: userId, tagId: tagId);
    try {
      final raw = await _storage.read(key: key);
      if (raw == null || raw.isEmpty) return null;
      final decoded = jsonDecode(raw) as Map<String, dynamic>;
      return PortfolioSnapshot.fromJson(decoded);
    } catch (_) {
      return null;
    }
  }

  /// Remove todos os snapshots criptografados armazenados no dispositivo.
  Future<void> clearAll() {
    return _synchronized(() async {
      final keysToDelete = <String>{..._trackedKeys};
      try {
        final allEntries = await _storage.readAll();
        for (final key in allEntries.keys) {
          if (key.startsWith(_keyPrefix)) {
            keysToDelete.add(key);
          }
        }
      } catch (_) {
        // Fallback para ambientes onde readAll() não é suportado ou mockado
      }

      for (final key in keysToDelete) {
        await _storage.delete(key: key);
      }
      _trackedKeys.clear();
    });
  }
}
