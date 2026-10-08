import { Card } from "./ui/card";
import { Progress } from "./ui/progress";
import { Badge } from "./ui/badge";
import Icon from "./Icon";
import { formatMoney } from "@/lib/money";
import CheckmarkCircle02Icon from "@hugeicons/core-free-icons/CheckmarkCircle02Icon";

const GoalCard = ({
  name,
  targetMinor,
  savedMinor,
  targetDate,
  currency,
}: {
  name: string;
  targetMinor: number;
  savedMinor: number;
  targetDate: string;
  currency: string;
}) => {
  const pct = Math.min(100, Math.round((savedMinor / targetMinor) * 100));
  const complete = pct >= 100;

  return (
    <Card className="flex flex-col gap-4 rounded-xl border border-gray-200 p-5 shadow-chart">
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="text-16 font-semibold text-gray-900">{name}</h3>
          <p className="text-12 text-gray-500">{complete ? "Goal reached" : `Target: ${targetDate}`}</p>
        </div>
        {complete && (
          <Badge className="gap-1 border-transparent bg-success-25 text-success-700 hover:bg-success-25">
            <Icon icon={CheckmarkCircle02Icon} size={14} />
            Done
          </Badge>
        )}
      </div>

      <div className="flex flex-col gap-2">
        <Progress value={pct} className="h-2 bg-gray-100" indicatorClassName={complete ? "bg-success-600" : "bg-bank-gradient"} />
        <div className="flex items-baseline justify-between">
          <p className="text-14 font-semibold text-gray-900">{formatMoney(savedMinor, currency)}</p>
          <p className="text-12 text-gray-500">of {formatMoney(targetMinor, currency)} · {pct}%</p>
        </div>
      </div>
    </Card>
  );
};

export default GoalCard;
