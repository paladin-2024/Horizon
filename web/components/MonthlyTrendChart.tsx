'use client'
import { useEffect, useRef, useState } from "react";
import { Bar, BarChart, CartesianGrid, Cell, LabelList, XAxis, YAxis } from "recharts";
import { ChartContainer, type ChartConfig } from "./ui/chart";
import { formatMoney } from "@/lib/money";
import { cn } from "@/lib/utils";

type Metric = "income" | "spending";

const METRIC_LABEL: Record<Metric, string> = { income: "Income", spending: "Spending" };

const config: ChartConfig = { value: { label: "Amount" } };

/**
 * A single-series bar chart with every bar in a quiet neutral tint except the
 * most recent month, which carries Bank Blue and a floating value callout —
 * the structural pattern from the dashboard references, rendered in Horizon's
 * own palette rather than the reference's colors (see DESIGN.md).
 */
const MonthlyTrendChart = ({
  data,
  currency,
}: {
  data: { month: string; income: number; spending: number }[];
  currency: string;
}) => {
  const [metric, setMetric] = useState<Metric>("income");
  const containerRef = useRef<HTMLDivElement>(null);
  const [chartWidth, setChartWidth] = useState(0);
  useEffect(() => {
    if (!containerRef.current) return;
    const observer = new ResizeObserver((entries) => setChartWidth(entries[0].contentRect.width));
    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, []);
  const highlightIndex = data.length - 1;
  const chartData = data.map((d, i) => ({
    month: d.month,
    value: d[metric],
    fill: i === highlightIndex ? "#0179FE" : "#EAECF0",
  }));

  const Callout = (props: {
    x?: string | number;
    y?: string | number;
    width?: string | number;
    index?: number;
    value?: string | number;
  }) => {
    if (props.index !== highlightIndex) return <g />;
    const x = Number(props.x ?? 0);
    const y = Number(props.y ?? 0);
    const width = Number(props.width ?? 0);
    const value = Number(props.value ?? 0);
    const cx = x + width / 2;
    const text = formatMoney(value, currency);
    const boxWidth = Math.max(80, text.length * 6.5 + 20);
    // Clamp the bubble inside the chart's own width (margin left/right: 4) so it
    // never clips off-screen when the highlighted bar sits near the right edge —
    // the common case, since the highlight is always the most recent month.
    const maxLeft = chartWidth > 0 ? chartWidth - boxWidth - 4 : Infinity;
    const left = Math.min(Math.max(cx - boxWidth / 2, 4), maxLeft);
    const boxCenter = left + boxWidth / 2;
    return (
      <g>
        <rect x={left} y={y - 34} width={boxWidth} height={26} rx={8} fill="#101828" />
        <polygon points={`${cx - 5},${y - 8} ${cx + 5},${y - 8} ${cx},${y - 2}`} fill="#101828" />
        <text x={boxCenter} y={y - 17} textAnchor="middle" fill="#fff" fontSize={12} fontWeight={600} fontFamily="var(--font-sans)">
          {text}
        </text>
      </g>
    );
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex w-fit rounded-full bg-gray-100 p-1">
        {(["income", "spending"] as Metric[]).map((m) => (
          <button
            key={m}
            type="button"
            onClick={() => setMetric(m)}
            className={cn(
              "rounded-full px-3 py-1 text-12 font-semibold transition-colors",
              metric === m ? "bg-gray-900 text-white" : "text-gray-500 hover:text-gray-700"
            )}
          >
            {METRIC_LABEL[m]}
          </button>
        ))}
      </div>

      <ChartContainer ref={containerRef} config={config} className="aspect-auto h-64 w-full">
        <BarChart data={chartData} margin={{ top: 28, right: 4, left: 4, bottom: 4 }}>
          <CartesianGrid vertical={false} stroke="#F2F4F7" />
          <XAxis
            dataKey="month"
            tickLine={false}
            axisLine={false}
            tick={{ fontFamily: "var(--font-sans)", fontSize: 12, fill: "#667085" }}
          />
          <YAxis
            tickLine={false}
            axisLine={false}
            tick={{ fontFamily: "var(--font-sans)", fontSize: 11, fill: "#98A2B3" }}
            tickFormatter={(v) => `${Number(v) / 100_000_000}M`}
          />
          <Bar dataKey="value" radius={6} maxBarSize={28}>
            {chartData.map((entry) => (
              <Cell key={entry.month} fill={entry.fill} />
            ))}
            <LabelList dataKey="value" content={Callout} />
          </Bar>
        </BarChart>
      </ChartContainer>
    </div>
  );
};

export default MonthlyTrendChart;
