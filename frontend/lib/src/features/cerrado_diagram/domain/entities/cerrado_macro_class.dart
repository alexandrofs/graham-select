/// As 5 Macro Classes oficiais do Diagrama do Cerrado.
enum CerradoMacroClass {
  acoesBr(
    label: 'Ações BR',
    jsonKey: 'targetAcoesBr',
  ),
  fiis(
    label: 'FIIs',
    jsonKey: 'targetFiis',
  ),
  rendaFixa(
    label: 'Renda Fixa / Tesouro',
    jsonKey: 'targetRendaFixa',
  ),
  internacional(
    label: 'Internacional',
    jsonKey: 'targetInternacional',
  ),
  cripto(
    label: 'Criptoativos',
    jsonKey: 'targetCripto',
  );

  const CerradoMacroClass({
    required this.label,
    required this.jsonKey,
  });

  final String label;
  final String jsonKey;
}
