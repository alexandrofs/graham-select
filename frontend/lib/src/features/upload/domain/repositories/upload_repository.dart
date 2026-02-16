import 'dart:io';
import '../entities/upload_result.dart';

abstract class UploadRepository {
  Future<UploadResult> uploadFile(File file);
}
