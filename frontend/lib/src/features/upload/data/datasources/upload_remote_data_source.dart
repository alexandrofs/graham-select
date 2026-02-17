import 'dart:convert';
import 'package:http/http.dart' as http;
import '../../domain/entities/app_file.dart';
import '../models/upload_response_model.dart';

class UploadRemoteDataSource {
  final http.Client client;
  final String baseUrl;

  const UploadRemoteDataSource({
    required this.client,
    this.baseUrl = 'http://localhost:8080/api/v1',
  });

  Future<UploadResponseModel> uploadFile(AppFile file) async {
    try {
      final uri = Uri.parse('$baseUrl/upload-financial-data');
      final request = http.MultipartRequest('POST', uri);

      // Adicionar o arquivo como multipart usando os bytes
      request.files.add(
        http.MultipartFile.fromBytes(
          'file',
          file.bytes,
          filename: file.name,
        ),
      );

      // Enviar requisição
      final streamedResponse = await request.send();
      final response = await http.Response.fromStream(streamedResponse);

      // Parsear resposta
      final Map<String, dynamic> jsonResponse = response.body.isNotEmpty
          ? jsonDecode(response.body) as Map<String, dynamic>
          : {};

      return UploadResponseModel.fromJson(jsonResponse, response.statusCode);
    } catch (e) {
      // Em caso de erro de rede ou outro erro
      throw Exception('Erro ao fazer upload: ${e.toString()}');
    }
  }
}
