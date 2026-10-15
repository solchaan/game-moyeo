import type { Position } from './types'

// Compact lane and role silhouettes, using the surrounding occupancy color.
export default function PositionIcon({position}:{position:Position}) {
  return <svg viewBox="0 0 32 32" width="32" height="32" fill="none" aria-hidden="true" focusable="false">
    {position==='TOP'&&<><path d="M6 26V6h20" stroke="currentColor" strokeWidth="5"/><path d="M12 26h14V12" stroke="currentColor" strokeWidth="2" opacity=".3"/><path d="m12 20 8-8" stroke="currentColor" strokeWidth="3"/></>}
    {position==='MID'&&<><path d="M6 19V6h13M13 26h13V13" stroke="currentColor" strokeWidth="2" opacity=".3"/><path d="m7 25 18-18" stroke="currentColor" strokeWidth="6"/></>}
    {position==='ADC'&&<><path d="M6 20V6h14" stroke="currentColor" strokeWidth="2" opacity=".3"/><path d="M6 26h20V6" stroke="currentColor" strokeWidth="5"/><path d="m12 20 8-8" stroke="currentColor" strokeWidth="3"/></>}
    {position==='JUNGLE'&&<path d="M16 28C5 23 4 15 5 6l7 10L10 3l7 13 6-12-1 14 6-9c1 9-4 15-12 19Z" fill="currentColor"/>}
    {position==='SUPPORT'&&<><path d="m16 4 4 6-4 6-4-6Z" fill="currentColor"/><path d="m3 11 9 5h8l9-5-4 10-7 1-2 7-2-7-7-1Z" fill="currentColor"/></>}
  </svg>
}
