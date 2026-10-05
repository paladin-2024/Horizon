'use client'
import { Chart as ChartJS, ArcElement, Tooltip } from "chart.js";
import { Doughnut } from "react-chartjs-2";
import { formatMoney } from "@/lib/money";
import { cn } from "@/lib/utils";

ChartJS.register(ArcElement, Tooltip);

const TINT_HEX = { blue: "#2E90FA", success: "#039855", pink: "#EE46BC" } as const;
const TINT_CLASS = {
  blue: { bg: "bg-blue-25", text: "text-blue-900" },
  success: { bg: "bg-success-25", text: "text-success-900" },
  pink: { bg: "bg-pink-25", text: "text-pink-900" },
} as const;

const CategoryBreakdown = ({
  items,
  currency,
}: {
  items: { name: string; amountMinor: number; tint: keyof typeof TINT_HEX }[];
  currency: string;
}) => {
  const total = items.reduce((sum, i) => sum + i.amountMinor, 0);

  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center">
      <div className="relative size-36 shrink-0">
        <Doughnut
          data={{
            labels: items.map((i) => i.name),
            datasets: [
              {
                data: items.map((i) => i.amountMinor),
                backgroundColor: items.map((i) => TINT_HEX[i.tint]),
                borderWidth: 0,
              },
            ],
          }}
          options={{
            cutout: "72%",
            plugins: {
              legend: { display: false },
              tooltip: {
                backgroundColor: "#101828",
                padding: 10,
                cornerRadius: 8,
                callbacks: { label: (ctx) => formatMoney(Number(ctx.raw), currency) },
              },
            },
          }}
        />
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <p className="text-12 text-gray-500">Total spent</p>
          <p className="text-16 font-semibold text-gray-900">{formatMoney(total, currency)}</p>
        </div>
      </div>

      <div className="flex flex-1 flex-wrap gap-2">
        {items.map((item) => (
          <span
            key={item.name}
            className={cn(
              "flex items-center gap-1.5 rounded-full px-3 py-1.5 text-12 font-semibold",
              TINT_CLASS[item.tint].bg,
              TINT_CLASS[item.tint].text
            )}
          >
            {item.name} · {Math.round((item.amountMinor / total) * 100)}%
          </span>
        ))}
      </div>
    </div>
  );
};

export default CategoryBreakdown;
