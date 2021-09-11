import { HttpErrorResponse, HttpEvent, HttpEventType } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { BehaviorSubject, Subscription } from 'rxjs';
import { SubSink } from 'subsink';
import { NotificationType } from '../enum/notification-type.enum';
import { Role } from '../enum/role.enum';
import { CustomHttpResponse } from '../model/custom-http-response';
import { FileUploadStatus } from '../model/file-upload.status';
import { User } from '../model/user';
import { AuthenticationService } from '../service/authentication.service';
import { NotificationService } from '../service/notification.service';
import { UserService } from '../service/user.service';

@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css']
})
export class UserComponent implements OnInit, OnDestroy {

  private subs = new SubSink();

  private titleSubject = new BehaviorSubject<string>('Profile');
  public titleAction$ = this.titleSubject.asObservable();
  public users: User[] | undefined; 
  public  user: any;
  public refreshing: boolean | undefined;
  private subscriptions: Subscription[] = [];
  public selectedUser: User | undefined;
  public fileName: any | undefined;
  public profileImage: any | undefined;
  public editUser = new User();
  private currentUsername: string | undefined;
  public fileStatus = new FileUploadStatus();

  constructor(private router: Router,private authenticationService: AuthenticationService, private userService: UserService, private notificationService: NotificationService) { }

  ngOnInit(): void {
    this.user = this.authenticationService.getUserFromLocalCache();
    this.getUsers(true);
  }

  public changeTitle(tilte:string): void{
 
    this.titleSubject.next(tilte);

  }

  public getUsers(showNotification: boolean): void {
    this.refreshing = true;
      this.subscriptions.push(
        this.userService.getUsers().subscribe(
          (response: User[]|any) => {
            this.userService.addUsersToLocalCache(response);
            this.users = response;
            this.refreshing = false;
            if (showNotification) {
              this.sendNotification(NotificationType.SUCCESS, `${response.length} user(s) loaded successfully.`);
            }
          },
          (errorResponse: HttpErrorResponse) => {
            this.sendNotification(NotificationType.ERROR, errorResponse.error.message);
            this.refreshing = false;
          }
        )
      );
  }

  public onSelectUser(selectedUser: User): void {
    this.selectedUser = selectedUser;
    this.clickButton('openUserInfo');
  }

  public onProfileImageChange(event: any):void {
    let files = event.target.files[0];
    let fileNames = files.name;
    this.fileName = fileNames;
    this.profileImage = files;

  }

  public saveNewUser(): void{
    this.clickButton('new-user-save');
  }

  public onAddNewUser(userForm: NgForm): void {
    const formData = this.userService.createUserFormData(null, userForm.value, this.profileImage);
    this.subscriptions.push(
      this.userService.addUser(formData).subscribe(
        (response:User | any) => {
          this.clickButton('new-user-close');
          this.getUsers(false);
          this.fileName = null;
          this.profileImage = null;
          userForm.reset();
          this.sendNotification(NotificationType.SUCCESS, `${response.firstName} ${response.lastName} added  successfully`);
        },
        (errorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, errorResponse.error.message);
        }

      )
    );
  }

  public onUpdateUser(): void {
    const formData = this.userService.createUserFormData(this.currentUsername, this.editUser, this.profileImage);
    this.subscriptions.push(
      this.userService.updateUser(formData).subscribe(
        (response:User | any) => {
          this.clickButton('closeEditUserModalButton');
          this.getUsers(false);
          this.fileName = null;
          this.profileImage = null;
          this.sendNotification(NotificationType.SUCCESS, `${response.firstName} ${response.lastName} updated  successfully`);
        },
        (errorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, errorResponse.error.message);
          this.profileImage = null;
        }

      )
    );
  }

  public onUpdateCurrentUser(user: User): void {
    this.refreshing = true;
    this.currentUsername = this.authenticationService.getUserFromLocalCache().username;
    const formData = this.userService.createUserFormData(this.currentUsername, user, this.profileImage);
    this.subscriptions.push(
      this.userService.updateUser(formData).subscribe(
        (response:User | any) => {
          this.authenticationService.addUserToLocalCache(response);
          this.getUsers(false);
          this.fileName = null;
          this.profileImage = null;
          this.sendNotification(NotificationType.SUCCESS, `${response.firstName} ${response.lastName} updated  successfully`);
        },
        (errorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, errorResponse.error.message);
          this.refreshing = true;
          this.profileImage = null;

        }

      )
    );
  }

  public onUpdateProfileImage(): void {
    const formData = new FormData();
    formData.append('username', this.user.username);
    formData.append('profileImage', this.profileImage);

    this.subscriptions.push(
      this.userService.updateProfileImage(formData).subscribe(
        (event:HttpEvent<any> | any) => {
         this.reportUploadProgress(event);
       
        },
        (errorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, errorResponse.error.message);
          this.fileStatus.status = 'done';
        }

      )
      );
 
  }
  private reportUploadProgress(event: any): void {

     switch(event.type){
       case HttpEventType.UploadProgress:
         this.fileStatus.percentage = Math.round(100 * event.loaded / event.total );
         this.fileStatus.status = 'progress';
         break;
       case HttpEventType.Response:
         if (event.status == 200){
           this.user.profileImageUrl = `${event.body.profileImageUrl}?time=${new Date().getTime()}`;
           this.sendNotification(NotificationType.SUCCESS, `${event.body.firstName}\'s profile image updated successfully`);
           this.fileStatus.status = 'done';
           break;
         }else{
          this.sendNotification(NotificationType.ERROR, `Unable to upload image. Please try again`);

          break;
         }
         default:
            `Finished all processes`;
     }

  }

  public updateProfileImage(): void {
    this.clickButton('profile-image-input');
  }

  public onLogOut(): void {
    this.authenticationService.logOut();
    this.router.navigate(['/login']);
    this.sendNotification(NotificationType.SUCCESS, `You've been successfully logged out`);
  }

  public onResetPassword(emailForm: NgForm): void {
    this.refreshing = true;
    const emailAddress = emailForm.value['reset-password-email'];
    this.subscriptions.push(
      this.userService.resetPassword(emailAddress).subscribe(
        (response: CustomHttpResponse | any) => {
          this.sendNotification(NotificationType.SUCCESS, response.message);
          this.refreshing = false;
        },
        (errorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.WARNING, errorResponse.error.message);
          this.refreshing = false;
        },
        () => emailForm.reset()
      )
    );
  }

  public onDeleteUser(username: any): void {
    this.subscriptions.push(
      this.userService.deleteUser(username).subscribe(
        (response: CustomHttpResponse | any) => {
          this.sendNotification(NotificationType.SUCCESS, response.message);
          this.getUsers(false);
        },
        (errorResponse: HttpErrorResponse) => {
          this.sendNotification(NotificationType.ERROR, errorResponse.error.message);
        }
      )
    );
  }

  public searchUsers(searchTerm: string): void {

    const results: User[] = [];
    for (const user of this.userService.getUsersFromLocalCache()){
      if (user.firstName.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1 || 
          user.lastName.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1 ||
          user.username.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1 || 
          user.userId.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1){
            results.push(user);
      }
    }
    this.users = results;
    if (results.length === 0 || !searchTerm){
      this.users = this.userService.getUsersFromLocalCache(); 
    }
  }

  public onEditUser(editUser: User): void {
    this.editUser = editUser;
    this.currentUsername = editUser.username;
    this.clickButton('openUserEdit');
  }

  public get isAdmin(): boolean {
    return this.getUserRole() === Role.ADMIN || this.getUserRole() === Role.SUPER_ADMIN;
  }

  public get isAdminOrManager(): boolean {
    return this.isAdmin || this.getUserRole() === Role.MANAGER;
  }
 

  private getUserRole(): any {
    return this.authenticationService.getUserFromLocalCache().role;
  }

  private  sendNotification(notificationType: NotificationType, message: string): void {
    if (message) {
      this.notificationService.notify(notificationType,message);
    }else{
      this.notificationService.notify(notificationType,'An error occured. Please try again.');
    }
  }

 private clickButton(buttonId: string): void{
   document.getElementById(buttonId)?.click();
 }

 ngOnDestroy(): void {
  this.subscriptions.forEach(sub => sub.unsubscribe());
}
}
