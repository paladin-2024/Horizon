import Image from "next/image";
import { ReactNode } from "react";

interface EmptyStateProps {
  illustration: string;
  title: string;
  description: string;
  action?: ReactNode;
}

/**
 * The shared "nothing here yet" screen: an illustration, a plain-language
 * explanation, and an optional next step. Used wherever a page has no real
 * data yet instead of a bare placeholder string.
 */
const EmptyState = ({ illustration, title, description, action }: EmptyStateProps) => {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-6 rounded-xl border border-gray-200 px-6 py-12 text-center">
      <Image src={illustration} alt="" width={220} height={160} className="h-auto w-[220px]" />
      <div className="flex flex-col gap-2">
        <h2 className="text-18 font-semibold text-gray-900">{title}</h2>
        <p className="text-14 max-w-sm text-gray-600">{description}</p>
      </div>
      {action}
    </div>
  );
};

export default EmptyState;
