import { CalendarWeek, getCalendarWeek, incrementCalendarWeek } from '@util/date-helpers';
import { useState } from 'react';
import { Expandable } from '../models/expandable';

function initializeCalendarWeeks(): Expandable<CalendarWeek>[] {
    const today = new Date();
    const numberOfWeeks = today.getUTCDay() === 1 ? 4 : 5;
    const weeks: Expandable<CalendarWeek>[] = [];
    const currentCalendarWeek = getCalendarWeek(today);
    for (let i = 0; i < numberOfWeeks; i++) {
        weeks.push({
            ...incrementCalendarWeek(currentCalendarWeek, i),
            isExpanded: i === 0,
        });
    }
    return weeks;
}
export function useCalendarWeeks() {
    const [calendarWeeks, setCalendarWeeks] = useState<Expandable<CalendarWeek>[]>(() => initializeCalendarWeeks());
    const expandWeek = (state: boolean, index: number) => {
        setCalendarWeeks((prev) => prev.map((cw, i) => (i === index ? { ...cw, isExpanded: state } : cw)));
    };
    return {
        calendarWeeks,
        expandWeek,
    };
}
