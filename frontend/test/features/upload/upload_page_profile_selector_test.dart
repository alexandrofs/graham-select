import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:frontend/src/features/family_holding/domain/entities/family_profile_entity.dart';
import 'package:frontend/src/features/family_holding/domain/repositories/family_profile_repository.dart';
import 'package:frontend/src/features/family_holding/presentation/providers/family_profile_provider.dart';
import 'package:frontend/src/features/upload/domain/entities/app_file.dart';
import 'package:frontend/src/features/upload/domain/entities/upload_processing_status.dart';
import 'package:frontend/src/features/upload/domain/entities/upload_result.dart';
import 'package:frontend/src/features/upload/domain/repositories/upload_repository.dart';
import 'package:frontend/src/features/upload/domain/usecases/get_b3_upload_status_usecase.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_b3_file_usecase.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_file_usecase.dart';
import 'package:frontend/src/features/upload/presentation/pages/upload_page.dart';
import 'package:frontend/src/features/upload/presentation/providers/upload_provider.dart';

class FakeUploadRepository implements UploadRepository {
  String? lastProfileId;

  @override
  Future<UploadResult> uploadFile(AppFile file) async =>
      const UploadResult(success: true, message: 'ok');

  @override
  Future<UploadResult> uploadB3File(AppFile file, {String? profileId}) async {
    lastProfileId = profileId;
    return const UploadResult(success: true, message: 'enviado');
  }

  @override
  Future<UploadProcessingStatus> fetchB3UploadStatus(String correlationId) async {
    return UploadProcessingStatus(
      correlationId: correlationId,
      fileName: 'b3.xlsx',
      status: 'COMPLETED',
      processedRows: 1,
      successfulRows: 1,
      failedRows: 0,
      duplicatedRows: 0,
      message: 'ok',
    );
  }
}

class FakeFamilyRepo implements FamilyProfileRepository {
  @override
  Future<List<FamilyProfileEntity>> fetchProfiles() async => const [
        FamilyProfileEntity(
          id: 'prof-123',
          name: 'Maria Silva',
          cpfMasked: '***.982.247-**',
          relationship: 'Cônjuge',
          active: true,
        ),
      ];

  @override
  Future<FamilyProfileEntity> linkProfile({
    required String cpf,
    required String name,
    String? relationship,
  }) =>
      throw UnimplementedError();

  @override
  Future<void> unlinkProfile(String profileId) => throw UnimplementedError();
}

void main() {
  testWidgets('exibe seletor de perfil na UploadPage e envia profileId selecionado', (tester) async {
    final uploadRepo = FakeUploadRepository();
    final uploadProvider = UploadProvider(
      uploadFileUseCase: UploadFileUseCase(uploadRepo),
      uploadB3FileUseCase: UploadB3FileUseCase(uploadRepo),
      getB3UploadStatusUseCase: GetB3UploadStatusUseCase(uploadRepo),
    );
    uploadProvider.setFile(AppFile(name: 'b3.xlsx', bytes: Uint8List.fromList([1, 2, 3]), size: 3));

    final familyProvider = FamilyProfileProvider(repository: FakeFamilyRepo());

    await tester.pumpWidget(
      MaterialApp(
        home: MultiProvider(
          providers: [
            ChangeNotifierProvider<UploadProvider>.value(value: uploadProvider),
            ChangeNotifierProvider<FamilyProfileProvider>.value(value: familyProvider),
          ],
          child: const UploadPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final selectorFinder = find.byKey(const Key('upload_profile_selector'));
    expect(selectorFinder, findsOneWidget);

    await tester.tap(selectorFinder);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Maria Silva (***.982.247-**)').last);
    await tester.pumpAndSettle();

    expect(uploadProvider.selectedProfileId, 'prof-123');

    await uploadProvider.uploadB3File();
    expect(uploadRepo.lastProfileId, 'prof-123');
  });
}

