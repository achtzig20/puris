
export type CalendarWeek = {
  week: number;
  isoYear: number;
  startDate: Date;
}

export function getCalendarWeek(date: Date): CalendarWeek {
  const targetDate = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  targetDate.setUTCDate(targetDate.getUTCDate() + 4 - (targetDate.getUTCDay() || 7));
  const startOfTargetWeek = new Date(targetDate);
  startOfTargetWeek.setUTCDate(targetDate.getUTCDate() - 3);
  const isoYear = targetDate.getUTCFullYear();
  const firstThursday = new Date(Date.UTC(isoYear, 0, 4));
  const startOfFirstWeek = new Date(firstThursday);
  startOfFirstWeek.setUTCDate(firstThursday.getUTCDate() - (firstThursday.getUTCDay() || 7) + 1);
  const week = Math.ceil(((+targetDate - +startOfFirstWeek) / 86400000 + 1) / 7);
  return { week, isoYear, startDate: startOfTargetWeek };
}

export function incrementCalendarWeek(cw: CalendarWeek, amount: number) {
  const {week, isoYear} = cw;

  // Get the first Thursday of the ISO year
  const firstThursday = new Date(Date.UTC(isoYear, 0, 4));

  // Find the start of the first ISO week (Monday before the first Thursday)
  const startOfFirstWeek = new Date(firstThursday);
  startOfFirstWeek.setUTCDate(firstThursday.getUTCDate() - (firstThursday.getUTCDay() || 7) + 1);

  // Calculate the date of the Monday of the given week
  const currentWeekStart = new Date(startOfFirstWeek);
  currentWeekStart.setUTCDate(startOfFirstWeek.getUTCDate() + (week - 1) * 7);

  // Move to the next week
  const nextWeekStart = new Date(currentWeekStart);
  nextWeekStart.setUTCDate(currentWeekStart.getUTCDate() + 7 * amount);

  // Determine the ISO year of the next week
  const nextThursday = new Date(nextWeekStart);
  nextThursday.setUTCDate(nextWeekStart.getUTCDate() + 3 - (nextWeekStart.getUTCDay() || 7));
  const newIsoYear = nextThursday.getUTCFullYear();

  // Get the first Thursday of the new ISO year
  const newYearFirstThursday = new Date(Date.UTC(newIsoYear, 0, 4));

  // Find the start of the first ISO week of the new year
  const newYearStartOfFirstWeek = new Date(newYearFirstThursday);
  newYearStartOfFirstWeek.setUTCDate(newYearFirstThursday.getUTCDate() - (newYearFirstThursday.getUTCDay() || 7) + 1);

  // Calculate the new week number
  const newWeekNumber = Math.ceil(((+nextWeekStart - +newYearStartOfFirstWeek) / 86400000 + 1) / 7);

  return { week: newWeekNumber, isoYear: newIsoYear, startDate: nextWeekStart };
}

export function incrementDate(date: Date, days: number) {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() + days);
  return newDate;
}
