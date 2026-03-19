package app.treasure.document.temporal;

public record DocumentProcessingRequest(
	Long documentId,
	Long organizationId)
{
}
