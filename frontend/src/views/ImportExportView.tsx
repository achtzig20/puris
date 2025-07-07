/*
Copyright (c) 2022 Volkswagen AG
Copyright (c) 2022 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2022 Contributors to the Eclipse Foundation

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

import { DropArea } from '@catena-x/portal-shared-components';
import { Box, Link, Stack, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { useTitle } from '@contexts/titleProvider';
import { useCallback, useEffect, useRef, useState } from 'react';
import React from 'react';
import { uploadDocuments } from '@services/import-service';
import { useNotifications } from '@contexts/notificationContext';

const HiddenInput = styled('input')({
  display: 'none',
});

export const ImportExportView = () => {
    const { setTitle } = useTitle();
    const { notify } = useNotifications();
    const fileInputRef = useRef<HTMLInputElement>(null);

    const templateFiles = [
        '/delivery-template.xlsx',
        '/demand-template.xlsx',
        '/production-template.xlsx',
        '/stock-template.xlsx',
    ];

    const handleClick = () => {
        fileInputRef.current?.click();
    };

    const handleFiles = useCallback(async (files: FileList | null) => {
        if (!files || files.length === 0) return;

        const supportedExtensions = ['.xlsx', '.csv'];
        const fileArray = Array.from(files);
        const validFiles = fileArray.filter(file => {
            const isValidType = supportedExtensions.some(ext =>
                file.name.toLowerCase().endsWith(ext)
            );
            if (!isValidType) {
                notify({
                    title: 'Unsupported File Type',
                    description: `File "${file.name}" is not supported. Only .xlsx and .csv files are allowed.`,
                    severity: 'error',
                });
            }
            return isValidType;
        });

        if (validFiles.length === 0) return;

        const uploadPromises = validFiles.map(async (file) => {
            try {
                const result = await uploadDocuments(file);
                notify({
                    title: 'Upload Successful',
                    description: `${file.name}: ${result.message}`,
                    severity: 'success',
                });
                return { file: file.name, success: true };
            } catch (error: any) {
                notify({
                    title: 'Upload Failed',
                    description: `${file.name}: ${error.message}`,
                    severity: 'error',
                });
                return { file: file.name, success: false, error: error.message };
            }
        });

        try {
            await Promise.allSettled(uploadPromises);
        } catch (error) {
            console.error('Unexpected error during file processing:', error);
        }
    }, [notify]);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        handleFiles(e.target.files);
        e.target.value = '';
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        e.stopPropagation();
        handleFiles(e.dataTransfer.files);
    };

    // Prevent default behavior for drag events
    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
        e.stopPropagation();
    };

    useEffect(() => {
        setTitle('Import');
    }, [setTitle])
    return (
        <Box width="100%" sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <ConfidentialBanner />
            <Stack
                component="section"
                sx={{ 
                    bgcolor: 'white', 
                    gap: 3, 
                    boxSizing: 'border-box', 
                    border: '1px solid #DCDCDC', 
                    borderRadius: '0.75rem !important',
                    overflow: 'hidden', 
                    paddingBottom: 2 
                }}
            >
                <Box sx={{bgcolor:'#081f4b', display: 'flex', alignItems: 'center', px: 3 , minHeight: '2rem' }}>
                    <Typography variant="body2" color='white' >
                        Import
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', px: 3 }}>
                    <Typography variant="body2">
                        Drag and drop files to import. If any error occur, you will be notified. 
                    </Typography>
                    <Typography variant="body2" component="div">
                        {templateFiles.map((file, idx) => {
                            const fileName = file.substring(file.lastIndexOf('/') + 1);
                            return (
                                <React.Fragment key={file}>
                                    <Link
                                        href={file}
                                        download={fileName}
                                        underline="hover"
                                        aria-label={`Download ${fileName} template`}
                                    >
                                        {fileName}
                                    </Link>
                                    {idx < templateFiles.length - 1 && ' | '}
                                </React.Fragment>
                            );
                        })}
                    </Typography>
                </Box>
                <Box sx={{ px: 3}}>
                    <Box onClick={handleClick} onDrop={handleDrop} onDragOver={handleDragOver}>
                        <DropArea
                            size="normal"
                            error=""
                            translations={{
                            errorTitle: 'Sorry, something went wrong',
                            subTitle: 'Supports: xlsx, csv',
                            title: 'Drag & Drop or click to browse',
                            }}
                        />
                    </Box>
                    <HiddenInput
                    type="file"
                    ref={fileInputRef}
                    onChange={handleFileChange}
                    multiple
                    accept=".xlsx,.csv"
                    />
                </Box>
            </Stack>
        </Box>
    );
};


