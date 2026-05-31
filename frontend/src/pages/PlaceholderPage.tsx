interface PlaceholderPageProps {
  screenId: string;
  title: string;
}

export function PlaceholderPage({ screenId, title }: PlaceholderPageProps) {
  return (
    <div className="mx-auto max-w-[1200px] px-4 py-12 md:px-8">
      <p className="text-sm text-muted-gray">{screenId}</p>
      <h1 className="mt-2 text-2xl font-semibold text-charcoal">{title}</h1>
      <p className="mt-4 text-muted-gray">
        T01 부트스트랩 라우트 셸입니다. 후속 태스크에서 UI_SPEC 기준으로 구현합니다.
      </p>
    </div>
  );
}
