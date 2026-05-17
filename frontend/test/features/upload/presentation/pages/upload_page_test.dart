import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:frontend/src/features/upload/presentation/pages/upload_page.dart';
import 'package:frontend/src/features/upload/presentation/providers/upload_provider.dart';
import 'package:mockito/mockito.dart';
import 'package:frontend/src/features/upload/domain/usecases/get_b3_upload_status_usecase.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_file_usecase.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_b3_file_usecase.dart';
import 'package:frontend/src/features/upload/domain/repositories/upload_repository.dart';

class MockUploadRepository extends Mock implements UploadRepository {}

void main() {
  late UploadFileUseCase uploadFileUseCase;
  late UploadB3FileUseCase uploadB3FileUseCase;
  late GetB3UploadStatusUseCase getB3UploadStatusUseCase;
  late UploadProvider uploadProvider;
  late MockUploadRepository mockRepository;

  setUp(() {
    mockRepository = MockUploadRepository();
    uploadFileUseCase = UploadFileUseCase(mockRepository);
    uploadB3FileUseCase = UploadB3FileUseCase(mockRepository);
    getB3UploadStatusUseCase = GetB3UploadStatusUseCase(mockRepository);
    uploadProvider = UploadProvider(
      uploadFileUseCase: uploadFileUseCase,
      uploadB3FileUseCase: uploadB3FileUseCase,
      getB3UploadStatusUseCase: getB3UploadStatusUseCase,
    );
  });

  Widget createWidgetUnderTest() {
    return MaterialApp(
      home: ChangeNotifierProvider<UploadProvider>.value(
        value: uploadProvider,
        child: const UploadPage(),
      ),
    );
  }

  testWidgets('deve exibir o título e a zona de upload da B3', (WidgetTester tester) async {
    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();
    
    expect(find.text('Importar Planilha da B3'), findsOneWidget);
    expect(find.text('Arraste sua planilha da B3 aqui'), findsOneWidget);
  });
}
