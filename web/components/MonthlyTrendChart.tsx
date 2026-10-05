'use client'
import {
  Chart as ChartJS,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
} from "chart.js";
import { Bar } from "react-chartjs-2";
import { formatMoney } from "@/lib/money";

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend);

const MonthlyTrendChart = ({
  data,
  currency,
}: {
  data: { month: string; income: number; spending: number }[];
  currency: string;
}) => {
  return (
    <Bar
      data={{
        labels: data.map((d) => d.month),
        datasets: [
          {
            label: "Income",
            data: data.map((d) => d.income),
            backgroundColor: "#D1E9FF",
            borderRadius: 6,
            maxBarThickness: 22,
          },
          {
            label: "Spending",
            data: data.map((d) => d.spending),
            backgroundColor: "#0179FE",
            borderRadius: 6,
            maxBarThickness: 22,
          },
        ],
      }}
      options={{
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: { grid: { display: false }, ticks: { font: { family: "var(--font-sans)", size: 12 }, color: "#667085" } },
          y: {
            grid: { color: "#EAECF0" },
            ticks: {
              font: { family: "var(--font-sans)", size: 11 },
              color: "#98A2B3",
              callback: (v) => `${Number(v) / 100_000_000}M`,
            },
          },
        },
        plugins: {
          legend: {
            position: "bottom",
            labels: { font: { family: "var(--font-sans)", size: 12, weight: 600 }, boxWidth: 10, boxHeight: 10, usePointStyle: true, pointStyle: "circle" },
          },
          tooltip: {
            backgroundColor: "#101828",
            padding: 10,
            cornerRadius: 8,
            displayColors: false,
            titleFont: { family: "var(--font-sans)", weight: 600 },
            bodyFont: { family: "var(--font-sans)" },
            callbacks: {
              label: (ctx) => `${ctx.dataset.label}: ${formatMoney(Number(ctx.raw), currency)}`,
            },
          },
        },
      }}
    />
  );
};

export default MonthlyTrendChart;
