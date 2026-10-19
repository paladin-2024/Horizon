'use client'
import { Pie, PieChart, Cell } from "recharts";
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "./ui/chart";
import { formatMoney } from "@/lib/money";
import { cn } from "@/lib/utils";
import { Badge } from "./ui/badge";

const TINT_CLASS = {
  blue: { bg: "bg-blue-25", text: "text-blue-900" },
  success: { bg: "bg-success-25", text: "text-success-900" },
  pink: { bg: "bg-pink-25", text: "text-pink-900" },
} as const;

// A distinct hue per wedge — unlike the 3-tint badge palette, reused colors on
// non-adjacent pie slices read as a rendering bug, not a design choice.
const SLICE_PALETTE = ["#0179FE", "#039855", "#EE46BC", "#6172F3", "#98A2B3"];

const CategoryBreakdown = ({
  items,
  currency,
}: {
  items: { name: string; amountMinor: number; tint: keyof typeof TINT_CLASS }[];
  currency: string;
}) => {
  const total = items.reduce((sum, i) => sum + i.amountMinor, 0);
  const data = items.map((i, idx) => ({
    name: i.name,
    value: i.amountMinor,
    fill: SLICE_PALETTE[idx % SLICE_PALETTE.length],
  }));
  const config: ChartConfig = Object.fromEntries(data.map((d) => [d.name, { label: d.name }]));

  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center">
      <div className="relative size-36 shrink-0">
        <ChartContainer config={config} className="aspect-square h-full w-full">
          <PieChart>
            <ChartTooltip
              content={
                <ChartTooltipContent
                  hideLabel
                  formatter={(value, name) => (
                    <div className="flex w-full items-center justify-between gap-4">
                      <span className="text-muted-foreground">{name}</span>
                      <span className="font-medium text-foreground">{formatMoney(Number(value), currency)}</span>
                    </div>
                  )}
                />
              }
            />
            <Pie
              data={data}
              dataKey="value"
              nameKey="name"
              innerRadius="72%"
              strokeWidth={0}
              startAngle={90}
              endAngle={-270}
            >
              {data.map((entry) => (
                <Cell key={entry.name} fill={entry.fill} />
              ))}
            </Pie>
          </PieChart>
        </ChartContainer>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <p className="text-12 text-gray-500">Total spent</p>
          <p className="text-16 font-semibold text-gray-900">{formatMoney(total, currency)}</p>
        </div>
      </div>

      <div className="flex flex-1 flex-wrap gap-2">
        {items.map((item) => (
          <Badge
            key={item.name}
            className={cn(
              "gap-1.5 border-transparent px-3 py-1.5 text-12 font-semibold",
              TINT_CLASS[item.tint].bg,
              TINT_CLASS[item.tint].text,
              `hover:${TINT_CLASS[item.tint].bg}`
            )}
          >
            {item.name} · {Math.round((item.amountMinor / total) * 100)}%
          </Badge>
        ))}
      </div>
    </div>
  );
};

export default CategoryBreakdown;
