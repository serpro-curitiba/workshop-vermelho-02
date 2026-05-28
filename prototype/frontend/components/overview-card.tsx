type OverviewCardProps = {
  label: string;
  value: string;
  hint: string;
};

export function OverviewCard({ label, value, hint }: OverviewCardProps) {
  return (
    <article className="metric">
      <span className="eyebrow">{label}</span>
      <strong>{value}</strong>
      <p>{hint}</p>
    </article>
  );
}