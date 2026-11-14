package afsdigital.grahamselect.api.tags.infrastructure.spring;

import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.usecase.AssignAssetToTagUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.CreateTagUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.DeleteTagUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.GetAssetTagsUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.ListTagsUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.RenameTagUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.SetAssetTagsUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.UnassignAssetFromTagUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TagsConfiguration {

    @Bean
    public CreateTagUseCase createTagUseCase(PortfolioTagPort portfolioTagPort) {
        return new CreateTagUseCase(portfolioTagPort);
    }

    @Bean
    public ListTagsUseCase listTagsUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        return new ListTagsUseCase(portfolioTagPort, assetTagPort);
    }

    @Bean
    public GetAssetTagsUseCase getAssetTagsUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        return new GetAssetTagsUseCase(portfolioTagPort, assetTagPort);
    }

    @Bean
    public RenameTagUseCase renameTagUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        return new RenameTagUseCase(portfolioTagPort, assetTagPort);
    }

    @Bean
    public DeleteTagUseCase deleteTagUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        return new DeleteTagUseCase(portfolioTagPort, assetTagPort);
    }

    @Bean
    public SetAssetTagsUseCase setAssetTagsUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        return new SetAssetTagsUseCase(portfolioTagPort, assetTagPort);
    }

    @Bean
    public AssignAssetToTagUseCase assignAssetToTagUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        return new AssignAssetToTagUseCase(portfolioTagPort, assetTagPort);
    }

    @Bean
    public UnassignAssetFromTagUseCase unassignAssetFromTagUseCase(
            PortfolioTagPort portfolioTagPort,
            AssetTagPort assetTagPort
    ) {
        return new UnassignAssetFromTagUseCase(portfolioTagPort, assetTagPort);
    }
}
