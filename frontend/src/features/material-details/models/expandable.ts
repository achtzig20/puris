export type Expandable<T extends object> = {
  isExpanded: boolean;
} & T;
