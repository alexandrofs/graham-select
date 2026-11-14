import '../../domain/entities/portfolio_tag.dart';
import '../../domain/repositories/tag_repository.dart';
import '../datasources/tag_remote_datasource.dart';

class TagRepositoryImpl implements TagRepository {
  final TagRemoteDataSource remoteDataSource;

  const TagRepositoryImpl({required this.remoteDataSource});

  @override
  Future<List<PortfolioTag>> fetchTags() {
    return remoteDataSource.fetchTags();
  }

  @override
  Future<List<PortfolioTag>> fetchAssetTags(String ticker) {
    return remoteDataSource.fetchAssetTags(ticker);
  }

  @override
  Future<PortfolioTag> createTag({required String name, String? color}) {
    return remoteDataSource.createTag(name: name, color: color);
  }

  @override
  Future<PortfolioTag> updateTag({
    required String tagId,
    required String name,
    String? color,
  }) {
    return remoteDataSource.updateTag(tagId: tagId, name: name, color: color);
  }

  @override
  Future<void> deleteTag(String tagId) {
    return remoteDataSource.deleteTag(tagId);
  }

  @override
  Future<List<PortfolioTag>> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  }) {
    return remoteDataSource.setAssetTags(ticker: ticker, tagIds: tagIds);
  }

  @override
  Future<void> assignAssetToTag({
    required String tagId,
    required String ticker,
  }) {
    return remoteDataSource.assignAssetToTag(tagId: tagId, ticker: ticker);
  }

  @override
  Future<void> unassignAssetFromTag({
    required String tagId,
    required String ticker,
  }) {
    return remoteDataSource.unassignAssetFromTag(tagId: tagId, ticker: ticker);
  }
}
