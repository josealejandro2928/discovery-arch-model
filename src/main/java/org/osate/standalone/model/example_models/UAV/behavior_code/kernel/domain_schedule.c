// Copyright 2020 Adventium Labs

// This is a kernel data structure. 

#include <config.h>
#include <object/structures.h>
#include <model/statedata.h>

// Manually generated schedule for
//
// CASE Phase-2-UAV-Experimental-Platform-Transformed/UAV 
//

// The length is in seL4 ticks (2 ms default). This schedule should be
// generated based on data captured in the AADL model

// This schedule is single-rate, 2Hz

// Leave time between threads (allocate it to domain 0) in case we
// discover additional threads are necessary or that more processor
// time is needed for some of the threads.

// The following is the domain id, duration, and name of process (which is
// what is shown on the figure, so hopefully easier to visualize)

// Time increases going down (0ms at top, 499ms at end of last thread)

// Note that UXAS is actually a thread group, ToDo: Determine how to specify time
// for HAMR to use for thread group

const dschedule_t ksDomSchedule[] = { // (1 tick == 2ms)
    { .domain =  0, .length = 50}, // (100ms) Domain 0
    { .domain =  1, .length =  100}, // (  2ms) Pacer
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain =  2, .length = 100}, // ( 20ms) RADIO: process RadioDriver_Attestation.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain =  3, .length =  100}, // (  6ms) FC_UART: process UARTDriver.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain =  4, .length =  100}, // (  2ms) AM: process CASE_AttestationManager.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain =  5, .length =  100}, // (  2ms) AM_Gate: process CASE_AttestationGate.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain =  6, .length =  100}, // (  2ms) FLT_AReq: process CASE_Filter_AReq.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain =  7, .length =  100}, // (  2ms) FLT_LST: process CASE_Filter_LST.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain =  8, .length =  100}, // (  2ms) FLT_OR: process CASE_Filter_OR.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain =  9, .length = 100}, // (100ms) UXAS: process UxAS.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain = 10, .length =  100}, // (  2ms) FlyZones: process FlyZonesDatabase.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain = 11, .length =  100}, // (  2ms) MON_REQ: process CASE_Monitor_Req.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain = 12, .length =  100}, // (  2ms) FLT_ARes: process CASE_Filter_ARes.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain = 13, .length =  100}, // (  2ms) MON_GEO: process CASE_Monitor_Geo.Impl;
    { .domain =  0, .length =  50}, // ( 18ms) Domain 0
    { .domain = 14, .length = 100}, // ( 20ms) WPM: process WaypointPlanManagerService.Impl;
};

const word_t ksDomScheduleLength = sizeof(ksDomSchedule) / sizeof(dschedule_t);
