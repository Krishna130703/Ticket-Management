export default function ErrorAlert({ error, onDismiss }) {
  if (!error) {
    return null;
  }

  const message = typeof error === 'string' ? error : error.message;
  const fieldErrors = typeof error === 'object' && error.fieldErrors ? error.fieldErrors : [];

  return (
    <div className="alert alert-error" role="alert">
      <div className="alert-header">
        <strong>Error</strong>
        {onDismiss && (
          <button type="button" className="alert-dismiss" onClick={onDismiss} aria-label="Dismiss">
            ×
          </button>
        )}
      </div>
      <p>{message}</p>
      {fieldErrors.length > 0 && (
        <ul className="field-errors">
          {fieldErrors.map((fieldError) => (
            <li key={`${fieldError.field}-${fieldError.message}`}>
              <strong>{fieldError.field}:</strong> {fieldError.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
