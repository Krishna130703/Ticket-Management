export default function CommentForm({ onSubmit, submitting, error }) {
  return (
    <form
      className="comment-form"
      onSubmit={(event) => {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        const body = formData.get('body')?.toString().trim() ?? '';
        if (!body) {
          return;
        }
        onSubmit(body);
        event.currentTarget.reset();
      }}
    >
      <div className="form-field">
        <label htmlFor="comment-body">Add comment</label>
        <textarea
          id="comment-body"
          name="body"
          rows={3}
          required
          disabled={submitting}
          placeholder="Write a comment…"
        />
        {error && <p className="field-error">{error}</p>}
      </div>
      <button type="submit" className="btn btn-secondary" disabled={submitting}>
        {submitting ? 'Posting…' : 'Post comment'}
      </button>
    </form>
  );
}
