export class User {
    public userId: string | undefined;
    public firstName: string;
    public lastName: string;
    public username: string;
    public email: string;
    public lastLoginDate: Date | any;
    public lastLoginDateDisplay: Date | any;
    public joinDate: Date | any;
    public profileImageUrl: string | undefined;
    public active: boolean;
    public notLocked: boolean;
    public role: string;
    public authorities: [];

    constructor(){
        this.userId = '';
        this.firstName = '';
        this.lastName = '';
        this.username = '';
        this.email = '';
        this.lastLoginDateDisplay = null;
        this.joinDate = null;
        this.profileImageUrl = '';
        this.active = false;
        this.notLocked = false;
        this.role = '';
        this.authorities = [];
    }
}