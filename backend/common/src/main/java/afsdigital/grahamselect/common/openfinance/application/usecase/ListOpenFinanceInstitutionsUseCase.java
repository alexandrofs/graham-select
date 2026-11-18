package afsdigital.grahamselect.common.openfinance.application.usecase;

import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceInstitutionDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ListOpenFinanceInstitutionsUseCase {

    private static final List<OpenFinanceInstitutionDTO> SUPPORTED_INSTITUTIONS = List.of(
            new OpenFinanceInstitutionDTO(
                    "XP_INVESTIMENTOS",
                    "XP Investimentos",
                    "https://assets.grahamselect.com/institutions/xp.png",
                    true,
                    "Conexão Open Finance via OpenID FAPI"
            ),
            new OpenFinanceInstitutionDTO(
                    "BTG_PACTUAL",
                    "BTG Pactual",
                    "https://assets.grahamselect.com/institutions/btg.png",
                    true,
                    "Conexão Open Finance via OpenID FAPI"
            ),
            new OpenFinanceInstitutionDTO(
                    "NUBANK",
                    "Nubank / NuInvest",
                    "https://assets.grahamselect.com/institutions/nubank.png",
                    true,
                    "Conexão Open Finance via OpenID FAPI"
            ),
            new OpenFinanceInstitutionDTO(
                    "ITAU",
                    "Itaú Corretora",
                    "https://assets.grahamselect.com/institutions/itau.png",
                    true,
                    "Conexão Open Finance via OpenID FAPI"
            ),
            new OpenFinanceInstitutionDTO(
                    "INTER",
                    "Banco Inter",
                    "https://assets.grahamselect.com/institutions/inter.png",
                    true,
                    "Conexão Open Finance via OpenID FAPI"
            )
    );

    public List<OpenFinanceInstitutionDTO> execute() {
        log.info("Listando instituições Open Finance suportadas (total={})", SUPPORTED_INSTITUTIONS.size());
        return SUPPORTED_INSTITUTIONS;
    }
}
