'use client'
import { useRef, useState } from "react";
import {
  Chart as ChartJS,
  BarElement,
  CategoryScale,
  LinearScale,
} from "chart.js";
import { Bar } from "react-chartjs-2";
import { formatMoney } from "@/lib/money";
import { cn } from "@/lib/utils";

ChartJS.register(BarElement, CategoryScale, LinearScale);

type Metric = "income" | "spending";

const METRIC_LABEL: Record<Metric, string> = { income: "Income", spending: "Spending" };

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
  const chartRef = useRef<ChartJS<"bar"> | null>(null);
  const [callout, setCallout] = useState<{ x: number; y: number; label: string } | null>(null);

  const values = data.map((d) => d[metric]);
  const highlightIndex = values.length - 1;

  const updateCallout = () => {
    const chart = chartRef.current;
    if (!chart) return;
    const meta = chart.getDatasetMeta(0);
    const point = meta.data[highlightIndex] as unknown as { x: number; y: number } | undefined;
    if (!point) return;
    setCallout({ x: point.x, y: point.y, label: formatMoney(values[highlightIndex], currency) });
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

      <div className="relative h-64">
        {callout && (
          <div
            className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-lg bg-gray-900 px-2.5 py-1.5 text-12 font-semibold text-white shadow-chart"
            style={{ left: callout.x, top: callout.y - 10 }}
          >
            {callout.label}
            <span className="absolute left-1/2 top-full -translate-x-1/2 border-4 border-transparent border-t-gray-900" />
          </div>
        )}
        <Bar
          ref={chartRef}
          data={{
            labels: data.map((d) => d.month),
            datasets: [
              {
                data: values,
                backgroundColor: values.map((_, i) => (i === highlightIndex ? "#0179FE" : "#EAECF0")),
                borderRadius: 6,
                maxBarThickness: 28,
              },
            ],
          }}
          options={{
            responsive: true,
            maintainAspectRatio: false,
            animation: { onComplete: updateCallout },
            onResize: () => requestAnimationFrame(updateCallout),
            scales: {
              x: { grid: { display: false }, ticks: { font: { family: "var(--font-sans)", size: 12 }, color: "#667085" } },
              y: {
                grid: { color: "#F2F4F7" },
                ticks: {
                  font: { family: "var(--font-sans)", size: 11 },
                  color: "#98A2B3",
                  // v is minor units (×100), so this divisor reads it back in whole-currency millions.
                  callback: (v) => `${Number(v) / 100_000_000}M`,
                },
              },
            },
            plugins: { legend: { display: false }, tooltip: { enabled: false } },
          }}
        />
      </div>
    </div>
  );
};

export default MonthlyTrendChart;
