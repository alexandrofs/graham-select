import 'csv_downloader_stub.dart'
    if (dart.library.js_interop) 'csv_downloader_web.dart' as impl;

/// Downloads [content] as a UTF-8 CSV file named [fileName].
Future<void> downloadCsvFile({
  required String content,
  required String fileName,
}) {
  return impl.downloadCsvFile(content: content, fileName: fileName);
}
