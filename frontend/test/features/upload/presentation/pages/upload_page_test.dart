import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';
import 'package:frontend/src/features/upload/domain/entities/app_file.dart';
import 'package:frontend/src/features/upload/presentation/pages/upload_page.dart';
import 'package:frontend/src/features/upload/presentation/providers/upload_provider.dart';
import 'upload_page_test.mocks.dart';

@GenerateMocks([UploadProvider])
void main() {
  late MockUploadProvider mockProvider;

  Widget createWidgetUnderTest() {
    return MaterialApp(
      home: ChangeNotifierProvider<UploadProvider>.value(
        value: mockProvider,
        child: const UploadPage(),
      ),
    );
  }

  setUp(() {
    mockProvider = MockUploadProvider();
    
    // Default stubs
    when(mockProvider.state).thenReturn(UploadState.idle);
    when(mockProvider.selectedFile).thenReturn(null);
    when(mockProvider.errorMessage).thenReturn(null);
    when(mockProvider.successMessage).thenReturn(null);
    when(mockProvider.hasFile).thenReturn(false);
    when(mockProvider.canUpload).thenReturn(false);
  });

  testWidgets('Should show initial state correctly', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());

    expect(find.text('Upload de Dados Financeiros'), findsOneWidget);
    expect(find.text('Envie seus dados financeiros'), findsOneWidget);
    expect(find.text('Clique para selecionar arquivo'), findsOneWidget);
    expect(find.byIcon(Icons.cloud_upload_outlined), findsOneWidget);
  });

  testWidgets('Should show file info card when file is selected', (tester) async {
    // Arrange
    final testFile = AppFile(
      name: 'test.csv',
      bytes: Uint8List.fromList([1, 2, 3]),
      size: 1024,
    );
    when(mockProvider.state).thenReturn(UploadState.idle);
    when(mockProvider.selectedFile).thenReturn(testFile);
    when(mockProvider.hasFile).thenReturn(true);
    when(mockProvider.canUpload).thenReturn(true);

    // Act
    await tester.pumpWidget(createWidgetUnderTest());

    // Assert
    expect(find.text('test.csv'), findsOneWidget);
    expect(find.text('1.00 KB'), findsOneWidget);
    expect(find.byIcon(Icons.table_chart), findsOneWidget); // CSV icon
    expect(find.text('Fazer Upload'), findsOneWidget);
  });

  testWidgets('Should show progress indicator when uploading', (tester) async {
    // Arrange
    final testFile = AppFile(
      name: 'test.csv',
      bytes: Uint8List.fromList([1, 2, 3]),
      size: 1024,
    );
    when(mockProvider.state).thenReturn(UploadState.uploading);
    when(mockProvider.hasFile).thenReturn(true);
    when(mockProvider.selectedFile).thenReturn(testFile);

    // Act
    await tester.pumpWidget(createWidgetUnderTest());

    // Assert
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(find.text('Enviando arquivo...'), findsOneWidget);
    expect(find.text('Fazer Upload'), findsNothing); // Button should be hidden or disabled
  });

  testWidgets('Should show success message when upload succeeds', (tester) async {
    // Arrange
    final testFile = AppFile(
      name: 'test.csv',
      bytes: Uint8List.fromList([1, 2, 3]),
      size: 1024,
    );
    when(mockProvider.state).thenReturn(UploadState.success);
    when(mockProvider.successMessage).thenReturn('Arquivo processado com sucesso!');
    when(mockProvider.hasFile).thenReturn(true);
    when(mockProvider.selectedFile).thenReturn(testFile);

    // Act
    await tester.pumpWidget(createWidgetUnderTest());

    // Assert
    expect(find.text('Arquivo processado com sucesso!'), findsOneWidget);
    expect(find.byIcon(Icons.check_circle), findsOneWidget);
    expect(find.text('Enviar Outro Arquivo'), findsOneWidget);
  });

  testWidgets('Should show error message when upload fails', (tester) async {
    // Arrange
    final testFile = AppFile(
      name: 'test.csv',
      bytes: Uint8List.fromList([1, 2, 3]),
      size: 1024,
    );
    when(mockProvider.state).thenReturn(UploadState.error);
    when(mockProvider.errorMessage).thenReturn('Erro ao conectar ao servidor');
    when(mockProvider.hasFile).thenReturn(true);
    when(mockProvider.selectedFile).thenReturn(testFile);

    // Act
    await tester.pumpWidget(createWidgetUnderTest());

    // Assert
    expect(find.text('Erro ao conectar ao servidor'), findsOneWidget);
    expect(find.byIcon(Icons.error), findsOneWidget);
    expect(find.text('Tentar Novamente'), findsOneWidget);
  });
}
