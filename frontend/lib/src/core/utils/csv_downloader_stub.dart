import 'dart:convert';
import 'package:url_launcher/url_launcher.dart';

Future<void> downloadCsvFile({
  required String content,
  required String fileName,
}) async {
  final uri = Uri.dataFromString(
    content,
    mimeType: 'text/csv',
    encoding: utf8,
  );
  await launchUrl(uri);
}
