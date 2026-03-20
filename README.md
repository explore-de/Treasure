# Device-menegment project 💻
```
████████╗██████╗ ███████╗ █████╗ ███████╗██╗   ██╗██████╗ ███████╗    
╚══██╔══╝██╔══██╗██╔════╝██╔══██╗██╔════╝██║   ██║██╔══██╗██╔════╝    
   ██║   ██████╔╝█████╗  ███████║███████╗██║   ██║██████╔╝█████╗      
   ██║   ██╔══██╗██╔══╝  ██╔══██║╚════██║██║   ██║██╔══██╗██╔══╝      
   ██║   ██║  ██║███████╗██║  ██║███████║╚██████╔╝██║  ██║███████╗    
   ╚═╝   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝  
```

A device booking system for organizations.

---
## TODO 📋

- [x] Device entity — pickupTime, bookedBy, getters/setters
- [x] DeviceResource — repository injected, index() passes device list
- [x] index.html — Carbon table + "New device" button
- [x] create.html + POST endpoint — form saves to DB, redirects
- [x] Delete endpoint — POST /devices/{id}/delete + delete button
- [x] Rename project to treasure (remove all "fuggs" references)
- [x] Update README
- [x] Navigation menu — "Device" link in left sidebar → device list
- [x] Edit button — separate admin and user edit views (admin can edit "Booked by"; user cannot)
- [x] Auto status — device becomes unavailable when assigned, available when unassigned
- [x] fix a "New device" button
- [x] remove fluggs name from website
- [x] Claim button — lets a regular user instantly claim a device; if already claimed by someone else, button is disabled; if claimed by you, button unclaims it
- [x] make it not possible to create an unnnamed Device
- [x] Move buttons in order: claim - edit - delete
- [ ] make a good UI design
- [x] maybe make a history of user bookings (who booked what and when) in edit menu
- [x] add more informations about devices (when it was registered, who registered it, etc.)
- [x] add delete protection (Users cant delete a device if it is older than 7 days or was used by multible users)

---
## Requirements ⚙️

### Functional
- Users can see a list of all devices and their current status
- Users can add a new device by filling out a simple form
- Users can edit device details like name and pickup time
- Users can delete a device that is no longer needed
- When a device is assigned to someone it automatically becomes unavailable, when returned it becomes available again
- Users can see who has a device and when they are picking it up

### Non-Functional
- The app is built with Java and runs as a web application
- All data is saved to a database so nothing is lost on restart
- The interface should be clean and easy to use
- The app should be easy to set up and run locally
---


## User Stories 👤

- As a user, I want to see a list of all devices so that I know what is available and what is not
- As a user, I want to add a new device so that it can be tracked in the system
- As a user, I want to edit a device so that I can update its information if something changes
- As a user, I want to delete a device so that the list stays clean and up to date
- As a user, I want to see who has booked a device so that I know who is responsible for it
- As a user, I want the device status to update automatically so that I don't have to do it manually
- As a user, I want to see the pickup time of a device so that I know when it will be returned
---


## Data modeling 🧩
```mermaid
erDiagram
    USER {
        int userId
        string username
    }

    DEVICE {
        int deviceId
        string deviceName
        string status
    }

    BOOKING {
        int bookingId
        int userId
        int deviceId
        datetime pickupTime
    }

    USER ||--o{ BOOKING : "makes"
    DEVICE ||--o{ BOOKING : "assigned to"
