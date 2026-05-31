import {
  COMPANY_DETAIL_COLUMN,
  COMPANY_DETAIL_MAIN_GRID,
  COMPANY_DETAIL_PAGE_SHELL,
} from "@/components/company/companyDetailLayout";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";

export function CompanyDetailSkeleton() {
  return (
    <div className={`${COMPANY_DETAIL_PAGE_SHELL} space-y-8`}>
      <SkeletonBlock className="h-40 w-full rounded-xl" />
      <div className={COMPANY_DETAIL_MAIN_GRID}>
        <div className={`${COMPANY_DETAIL_COLUMN}`}>
          <SkeletonBlock className="h-52 w-full rounded-xl" />
          <SkeletonBlock className="h-64 w-full rounded-xl" />
          <SkeletonBlock className="h-48 w-full rounded-xl" />
        </div>
        <div className={`${COMPANY_DETAIL_COLUMN}`}>
          <SkeletonBlock className="h-56 w-full rounded-xl" />
          <SkeletonBlock className="h-36 w-full rounded-xl" />
          <SkeletonBlock className="h-48 w-full rounded-xl" />
        </div>
      </div>
    </div>
  );
}
