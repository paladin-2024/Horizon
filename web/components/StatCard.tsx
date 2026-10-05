import { IconSvgElement } from "@hugeicons/react";
import Icon from "./Icon";
import { Card } from "./ui/card";
import { cn } from "@/lib/utils";
import ArrowUp01Icon from "@hugeicons/core-free-icons/ArrowUp01Icon";
import ArrowDown01Icon from "@hugeicons/core-free-icons/ArrowDown01Icon";

const TINT = {
  blue: { bg: "bg-blue-25", text: "text-blue-700" },
  success: { bg: "bg-success-25", text: "text-success-700" },
  pink: { bg: "bg-pink-25", text: "text-pink-700" },
} as const;

/** The small icon-badge stat tile borrowed from dashboard-template conventions — see DESIGN.md. */
const StatCard = ({
  icon,
  label,
  value,
  tint = "blue",
  delta,
}: {
  icon: IconSvgElement;
  label: string;
  value: string;
  tint?: keyof typeof TINT;
  delta?: { direction: "up" | "down"; label: string };
}) => {
  return (
    <Card className="flex items-center gap-4 rounded-xl border border-gray-200 p-4 shadow-chart sm:p-5">
      <span className={cn("flex size-10 shrink-0 items-center justify-center rounded-full", TINT[tint].bg)}>
        <Icon icon={icon} size={20} className={TINT[tint].text} />
      </span>
      <div className="flex min-w-0 flex-col gap-1">
        <p className="text-12 font-medium text-gray-500">{label}</p>
        <div className="flex items-baseline gap-2">
          <p className="text-18 font-semibold text-gray-900">{value}</p>
          {delta && (
            <span
              className={cn(
                "text-10 flex shrink-0 items-center gap-0.5 whitespace-nowrap rounded-full px-1.5 py-0.5 font-semibold",
                delta.direction === "up" ? "bg-success-25 text-success-700" : "bg-pink-25 text-pink-700"
              )}
            >
              <Icon icon={delta.direction === "up" ? ArrowUp01Icon : ArrowDown01Icon} size={10} />
              {delta.label}
            </span>
          )}
        </div>
      </div>
    </Card>
  );
};

export default StatCard;
