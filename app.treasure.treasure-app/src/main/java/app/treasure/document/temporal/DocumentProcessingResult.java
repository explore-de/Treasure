package app.treasure.document.temporal;

import app.treasure.document.domain.ExtractionSource;

public record DocumentProcessingResult(
	String status,
	String error,
	ExtractionSource extractionSource)
{
}
