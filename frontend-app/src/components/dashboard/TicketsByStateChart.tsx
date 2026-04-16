import Chart from "react-apexcharts";
import { ApexOptions } from "apexcharts";
import type { TicketsByState } from "../../types/dashboard";

interface TicketsByStateChartProps {
  data: TicketsByState;
}

export default function TicketsByStateChart({ data }: TicketsByStateChartProps) {
  const categories = [
    "New",
    "Assigned",
    "Planned",
    "Pending",
    "Solved",
    "Closed",
  ];

  const values = [
    data.new,
    data.processingAssigned,
    data.processingPlanned,
    data.pending,
    data.solved,
    data.closed,
  ];

  const options: ApexOptions = {
    colors: ["#465fff", "#22c55e", "#f59e0b", "#8b5cf6", "#06b6d4", "#6b7280"],
    chart: {
      fontFamily: "Outfit, sans-serif",
      type: "bar",
      height: 300,
      toolbar: { show: false },
    },
    plotOptions: {
      bar: {
        horizontal: false,
        columnWidth: "50%",
        borderRadius: 5,
        borderRadiusApplication: "end",
        distributed: true,
      },
    },
    dataLabels: { enabled: false },
    stroke: { show: true, width: 4, colors: ["transparent"] },
    xaxis: {
      categories,
      axisBorder: { show: false },
      axisTicks: { show: false },
    },
    legend: { show: false },
    yaxis: { title: { text: "Tickets" } },
    grid: { yaxis: { lines: { show: true } } },
    fill: { opacity: 1 },
    tooltip: {
      y: { formatter: (val: number) => `${val} tickets` },
    },
  };

  const series = [{ name: "Tickets", data: values }];

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-4">
        Tickets by State
      </h3>
      <Chart options={options} series={series} type="bar" height={300} />
    </div>
  );
}
