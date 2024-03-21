/*
Copyright (c) 2022,2024 Volkswagen AG
Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2022,2024 Contributors to the Eclipse Foundation

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

import Card from '@mui/material/Card';

import { useNegotiations } from '@hooks/edc/useNegotiations';
import { Negotiation } from '@models/types/edc/negotiation';
import { Box, List, ListItem, Typography } from '@mui/material';

type NegotiationCardProps = {
    negotiation: Negotiation;
};

const NegotiationCard = ({negotiation }: NegotiationCardProps) => {
    return (
        <Card sx={{ padding: '1.25rem' }}>
            <Typography variant="h6" mb="0.5rem" fontWeight="600">Negotiation</Typography>
            <Box sx={{
                display: 'flex',
                width: '100%',
                flexDirection: 'column',
                gap: '1rem'
            }}>
                <Box sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.5rem'
                }}>
                    <Box display="flex" gap="1rem">
                        <Typography variant="body1" fontWeight="600" width="30ch"> Negotiation Id: </Typography>
                        {negotiation['@id']}
                    </Box>
                    <Box display="flex" gap="1rem">
                        <Typography variant="body1" fontWeight="600" width="30ch"> Agreement  Id: </Typography>
                        <Typography variant="body1" sx={{ wordBreak: 'break-all', width: '60ch' }}>{negotiation['edc:contractAgreementId']}</Typography>
                    </Box>
                    <Box display="flex" gap="1rem">
                        <Typography variant="body1" fontWeight="600" width="30ch"> Type: </Typography>
                        {negotiation['edc:type']}
                    </Box>
                    <Box display="flex" gap="1rem">
                        <Typography variant="body1" fontWeight="600" width="30ch"> State: </Typography>
                        {negotiation['edc:state']}
                    </Box>
                    <Box display="flex" gap="1rem">
                        <Typography variant="body1" fontWeight="600" width="30ch"> CounterParty: </Typography>
                        {negotiation['edc:counterPartyId']}
                    </Box>
                    <Box display="flex" gap="1rem">
                        <Typography variant="body1" fontWeight="600" width="30ch"> Counterparty EDC URL: </Typography>
                        {negotiation['edc:counterPartyAddress']}
                    </Box>
                    <Box display="flex" gap="1rem">
                        <Typography variant="body1" fontWeight="600" width="30ch"> Timestamp: </Typography>
                        {new Date(negotiation['edc:createdAt']).toLocaleString()}
                    </Box>
                </Box>
            </Box>
        </Card>
    );
};

export const NegotiationView = () => {
    const { negotiations } = useNegotiations();
    return (
        <Box sx={{
            display: 'flex',
            height: '100%',
            width: '100%',
            flexDirection: 'column',
            alignItems: 'center'
        }}>
            <Typography variant="h4" mb="1rem">Negotiation</Typography>
            <List sx={{
                display: 'flex',
                flexDirection: 'column',
                gap: '1rem',
                width: '100ch'
            }}>
                {negotiations && negotiations.length > 0 ? (
                    negotiations.map((negotiation) => (
                        <ListItem>
                            <NegotiationCard negotiation={negotiation} />
                        </ListItem>
                    ))
                ) : (
                    <Typography variant="body1" align="center">No negotiations found. This list will be updated when Negotiations happen.</Typography>
                )}
            </List>
        </Box>
    );
}
