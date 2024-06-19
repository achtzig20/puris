/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
export const timeAgo = (date1: Date, date2: Date) => {
    // Calculate the difference in milliseconds
    const diff = Math.abs(date2.getTime() - date1.getTime());
    // Convert the difference to seconds
    let diffInSeconds = Math.floor(diff / 1000);
    // Calculate days, hours, minutes, and seconds
    const days = Math.floor(diffInSeconds / (3600 * 24));
    diffInSeconds -= days * 3600 * 24;
    const hours = Math.floor(diffInSeconds / 3600);
    diffInSeconds -= hours * 3600;
    const minutes = Math.floor(diffInSeconds / 60);
    const seconds = diffInSeconds - minutes * 60;
    // Determine the appropriate format
    if (days > 0) {
        return `${days} day(s) ago`;
    } else if (hours > 0) {
        return `${hours} hour(s) ago`;
    } else if (minutes > 0) {
        return `${minutes} minute(s) ago`;
    } else {
        return `${seconds} second(s) ago`;
    }
}
