import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "CampusOne Admin Portal — HNB Garhwal University",
  description:
    "Role-scoped university administration portal for HNBGU departments, notices, timetables, assignments, exams, attendance, and push notifications.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="antialiased">{children}</body>
    </html>
  );
}
