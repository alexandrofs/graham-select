import '../entities/portfolio_tag.dart';

abstract class TagRepository {
  Future<List<PortfolioTag>> fetchTags();

  Future<List<PortfolioTag>> fetchAssetTags(String ticker);

  Future<PortfolioTag> createTag({required String name, String? color});

  Future<PortfolioTag> updateTag({
    required String tagId,
    required String name,
    String? color,
  });

  Future<void> deleteTag(String tagId);

  Future<List<PortfolioTag>> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  });

  Future<void> assignAssetToTag({
    required String tagId,
    required String ticker,
  });

  Future<void> unassignAssetFromTag({
    required String tagId,
    required String ticker,
  });
}
