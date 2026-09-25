import { formatDate } from '../utils/formatDate.js';

export default function CommentList({ comments }) {
  if (!comments?.length) {
    return <p className="muted">No comments yet.</p>;
  }

  return (
    <ul className="comment-list">
      {comments.map((comment) => (
        <li key={comment.id} className="comment-item">
          <p className="comment-body">{comment.body}</p>
          <time className="comment-time" dateTime={comment.createdAt}>
            {formatDate(comment.createdAt)}
          </time>
        </li>
      ))}
    </ul>
  );
}
