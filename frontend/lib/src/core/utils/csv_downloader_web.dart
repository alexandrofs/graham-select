import 'dart:js_interop';
import 'dart:js_interop_unsafe';

@JS('Blob')
extension type _JSBlob._(JSObject _) implements JSObject {
  external factory _JSBlob(JSArray<JSString> parts, JSObject options);
}

@JS('URL.createObjectURL')
external JSString _createObjectUrl(JSObject blob);

@JS('URL.revokeObjectURL')
external void _revokeObjectUrl(JSString url);

/// Triggers a native browser file download via Blob + ObjectURL on Flutter Web.
Future<void> downloadCsvFile({
  required String content,
  required String fileName,
}) async {
  final options = JSObject();
  options.setProperty('type'.toJS, 'text/csv;charset=utf-8;'.toJS);
  final blob = _JSBlob(<JSString>['\uFEFF$content'.toJS].toJS, options);
  final objectUrl = _createObjectUrl(blob);

  final doc = globalContext.getProperty<JSObject>('document'.toJS);
  final body = doc.getProperty<JSObject?>('body'.toJS);
  final anchor = doc.callMethod<JSObject>('createElement'.toJS, 'a'.toJS);
  anchor.setProperty('href'.toJS, objectUrl);
  anchor.setProperty('download'.toJS, fileName.toJS);

  final style = anchor.getProperty<JSObject>('style'.toJS);
  style.setProperty('display'.toJS, 'none'.toJS);

  body?.callMethod<JSAny?>('appendChild'.toJS, anchor);
  try {
    anchor.callMethod<JSAny?>('click'.toJS);
  } finally {
    body?.callMethod<JSAny?>('removeChild'.toJS, anchor);
    _revokeObjectUrl(objectUrl);
  }
}
